// Package testhost is a client for the standalone Go email-verification test host.
package testhost

import (
	"bytes"
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
	"unicode"
)

// Attempt carries one client's proof. Keep it private and reuse it when retrying a start.
type Attempt struct {
	operation    string
	secret       string
	authorizedAt time.Time
}

// NewAttempt generates a new operation and proof at the given authorization time.
func NewAttempt(authorizedAt time.Time) (Attempt, error) {
	if authorizedAt.IsZero() {
		return Attempt{}, errors.New("testhost: authorization time is required")
	}
	var random [64]byte
	if _, err := rand.Read(random[:]); err != nil {
		return Attempt{}, err
	}
	return Attempt{
		operation:    base64.RawURLEncoding.EncodeToString(random[:32]),
		secret:       base64.RawURLEncoding.EncodeToString(random[32:]),
		authorizedAt: authorizedAt.UTC(),
	}, nil
}

// Operation is the client operation ID, without its proof.
func (a Attempt) Operation() string { return a.operation }

// Format keeps the proof out of fmt and structured log formatting.
func (a Attempt) Format(state fmt.State, _ rune) { io.WriteString(state, "[testhost attempt]") }

func (a Attempt) challenge() string {
	hash := sha256.Sum256([]byte(a.secret))
	return base64.RawURLEncoding.EncodeToString(hash[:])
}

// Result is the test host's public response. Only Accepted indicates final success.
type Result struct {
	Operation string `json:"operation"`
	State     string `json:"state"`
	Accepted  bool   `json:"accepted"`
}

// ResponseError reports the status and recognized API error without including the raw body.
type ResponseError struct {
	Status int
	Code   string
}

func (e *ResponseError) Error() string {
	return fmt.Sprintf("testhost: HTTP %d (%s)", e.Status, e.Code)
}

// Client sends authenticated requests to one HTTPS test-host origin.
type Client struct {
	origin *url.URL
	http   http.Client
	token  func(context.Context) (string, error)
}

// New returns a client. token supplies a Cognito access token for each request.
// Pass nil for transport to use http.DefaultTransport.
func New(origin string, transport http.RoundTripper, token func(context.Context) (string, error)) (*Client, error) {
	u, err := url.Parse(origin)
	if err != nil || u.Scheme != "https" || u.Hostname() == "" || u.User != nil ||
		u.RawQuery != "" || u.ForceQuery || strings.Contains(origin, "#") || (u.EscapedPath() != "" && u.EscapedPath() != "/") {
		return nil, errors.New("testhost: expected an HTTPS origin")
	}
	if port := u.Port(); port != "" {
		number, err := strconv.ParseUint(port, 10, 16)
		if err != nil || number == 0 {
			return nil, errors.New("testhost: invalid port")
		}
	} else if strings.HasSuffix(u.Host, ":") {
		return nil, errors.New("testhost: invalid port")
	}
	if token == nil {
		return nil, errors.New("testhost: token function is required")
	}
	return &Client{
		origin: u,
		token:  token,
		http: http.Client{
			Transport: transport,
			Timeout:   25 * time.Second,
			CheckRedirect: func(*http.Request, []*http.Request) error {
				return http.ErrUseLastResponse
			},
		},
	}, nil
}

type request struct {
	Operation    string `json:"operation"`
	Email        string `json:"email,omitempty"`
	Challenge    string `json:"challenge,omitempty"`
	AuthorizedAt string `json:"authorized_at,omitempty"`
	Secret       string `json:"secret,omitempty"`
	Code         string `json:"code,omitempty"`
}

func (c *Client) Start(ctx context.Context, attempt Attempt, email string) (Result, error) {
	if strings.TrimSpace(email) == "" {
		return Result{}, errors.New("testhost: email is required")
	}
	return c.post(ctx, "start", request{
		Operation: attempt.operation, Email: email, Challenge: attempt.challenge(),
		AuthorizedAt: attempt.authorizedAt.Format(time.RFC3339Nano),
	})
}

func (c *Client) Confirm(ctx context.Context, attempt Attempt, code string) (Result, error) {
	if len(code) != 6 || strings.IndexFunc(code, func(r rune) bool { return r < '0' || r > '9' }) >= 0 {
		return Result{}, errors.New("testhost: expected a six-digit code")
	}
	return c.post(ctx, "confirm", request{Operation: attempt.operation, Secret: attempt.secret, Code: code})
}

func (c *Client) Status(ctx context.Context, attempt Attempt) (Result, error) {
	return c.post(ctx, "status", request{Operation: attempt.operation})
}

func (c *Client) post(ctx context.Context, action string, in request) (Result, error) {
	if len(in.Operation) != 43 {
		return Result{}, errors.New("testhost: create an attempt first")
	}
	token, err := c.token(ctx)
	if err != nil {
		return Result{}, err
	}
	if token == "" || strings.IndexFunc(token, func(r rune) bool { return unicode.IsSpace(r) || unicode.IsControl(r) }) >= 0 {
		return Result{}, errors.New("testhost: invalid access token")
	}

	payload, err := json.Marshal(in)
	if err != nil {
		return Result{}, err
	}
	endpoint := c.origin.ResolveReference(&url.URL{Path: "/v1/verification/" + action})
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint.String(), bytes.NewReader(payload))
	if err != nil {
		return Result{}, err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+token)

	res, err := c.http.Do(req)
	if err != nil {
		return Result{}, err
	}
	defer res.Body.Close()
	if res.StatusCode == http.StatusUnauthorized || res.StatusCode == http.StatusForbidden {
		return Result{}, &ResponseError{Status: res.StatusCode, Code: "unauthorized"}
	}

	var out struct {
		Result
		Code string `json:"error"`
	}
	const maxBody = 8192
	data, err := io.ReadAll(io.LimitReader(res.Body, maxBody+1))
	if err != nil {
		return Result{}, err
	}
	if len(data) > maxBody || json.Unmarshal(data, &out) != nil {
		return Result{}, &ResponseError{Status: res.StatusCode, Code: "invalid_response"}
	}
	if res.StatusCode != http.StatusOK || out.Code != "" {
		switch out.Code {
		case "invalid", "expired", "attempts_exhausted", "retry_later", "unresolved", "temporarily_unavailable":
			return Result{}, &ResponseError{Status: res.StatusCode, Code: out.Code}
		default:
			return Result{}, &ResponseError{Status: res.StatusCode, Code: "unexpected_response"}
		}
	}
	if out.Operation != in.Operation || out.State == "" {
		return Result{}, &ResponseError{Status: res.StatusCode, Code: "invalid_response"}
	}
	return out.Result, nil
}
