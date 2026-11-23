package testhost

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"reflect"
	"strings"
	"testing"
	"time"
)

func TestVerificationProtocol(t *testing.T) {
	ctx := context.Background()
	attempt, err := NewAttempt(time.Date(2026, 11, 23, 16, 0, 0, 0, time.UTC))
	if err != nil {
		t.Fatal(err)
	}
	var calls int
	server := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost || r.Header.Get("Authorization") != "Bearer test-access-token" ||
			r.Header.Get("Content-Type") != "application/json" {
			t.Error("invalid request method or headers")
		}
		var in map[string]string
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			t.Error(err)
			w.WriteHeader(http.StatusBadRequest)
			return
		}
		calls++
		wantAction, state := "start", "pending"
		want := map[string]string{"operation": attempt.Operation()}
		switch calls {
		case 1, 2:
			want["email"] = "allowed@example.invalid"
			want["challenge"] = attempt.challenge()
			want["authorized_at"] = "2026-11-23T16:00:00Z"
		case 3:
			wantAction, state = "confirm", "confirmed"
			want["secret"], want["code"] = attempt.secret, "012345"
		case 4:
			wantAction, state = "status", "acknowledged"
		}
		if !reflect.DeepEqual(in, want) || r.URL.Path != "/v1/verification/"+wantAction {
			t.Errorf("invalid request for %s", wantAction)
		}
		json.NewEncoder(w).Encode(Result{Operation: attempt.Operation(), State: state, Accepted: calls == 4})
	}))
	defer server.Close()

	client, err := New(server.URL, server.Client().Transport, staticToken)
	if err != nil {
		t.Fatal(err)
	}
	for i := 0; i < 2; i++ {
		got, err := client.Start(ctx, attempt, "allowed@example.invalid")
		if err != nil || got.Accepted || got.State != "pending" {
			t.Fatalf("start: %+v, %v", got, err)
		}
	}
	got, err := client.Confirm(ctx, attempt, "012345")
	if err != nil || got.Accepted || got.State != "confirmed" {
		t.Fatalf("confirm: %+v, %v", got, err)
	}
	got, err = client.Status(ctx, attempt)
	if err != nil || !got.Accepted || got.State != "acknowledged" {
		t.Fatalf("status: %+v, %v", got, err)
	}
}

func TestChallengeHashesEncodedProof(t *testing.T) {
	attempt := Attempt{secret: "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
	if got, want := attempt.challenge(), "DwBzhbb51LfusnSGBa_hqYSgo7-j8BTQnip4TOnlzRo"; got != want {
		t.Fatalf("challenge = %s, want %s", got, want)
	}
	if strings.Contains(fmt.Sprintf("%#v", attempt), attempt.secret) {
		t.Fatal("attempt formatting leaked the proof")
	}
}

func TestErrorsAndRedirect(t *testing.T) {
	ctx := context.Background()
	attempt, err := NewAttempt(time.Now())
	if err != nil {
		t.Fatal(err)
	}
	server := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/redirected" {
			t.Error("client followed redirect")
			return
		}
		switch r.URL.Path {
		case "/v1/verification/start":
			http.Redirect(w, r, "/redirected", http.StatusTemporaryRedirect)
		case "/v1/verification/confirm":
			w.WriteHeader(http.StatusConflict)
			fmt.Fprint(w, `{"error":"retry_later"}`)
		case "/v1/verification/status":
			w.WriteHeader(http.StatusUnauthorized)
		}
	}))
	defer server.Close()

	for _, bad := range []string{"http://example.invalid", "https://u@example.invalid", "https://example.invalid/foo", "https://example.invalid?"} {
		if _, err := New(bad, nil, staticToken); err == nil {
			t.Errorf("unexpected valid origin %s", bad)
		}
	}
	client, err := New(server.URL, server.Client().Transport, staticToken)
	if err != nil {
		t.Fatal(err)
	}
	_, err = client.Start(ctx, attempt, "tester@example.invalid")
	var response *ResponseError
	if !errors.As(err, &response) || response.Status != http.StatusTemporaryRedirect {
		t.Fatalf("redirect: %v", err)
	}
	_, err = client.Confirm(ctx, attempt, "012345")
	if !errors.As(err, &response) || response.Code != "retry_later" {
		t.Fatalf("confirmation: %v", err)
	}
	_, err = client.Status(ctx, attempt)
	if !errors.As(err, &response) || response.Code != "unauthorized" {
		t.Fatalf("authorization: %v", err)
	}
}

func staticToken(context.Context) (string, error) { return "test-access-token", nil }
