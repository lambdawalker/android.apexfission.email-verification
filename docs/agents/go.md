# Integrate from Go

Import `github.com/lambdawalker/android.apexfission.email-verification/testhost` in Go 1.22+. While the project is in development, use a Git commit or a local checkout with a `go.mod` replacement. The Go package uses only the standard library.

```go
client, err := testhost.New(origin, nil, accessToken)
if err != nil {
    return err
}
attempt, err := testhost.NewAttempt(time.Now())
if err != nil {
    return err
}
if _, err := client.Start(ctx, attempt, recipient); err != nil {
    return err
}

// Call Confirm after reading the six-digit code from the permitted mailbox.
if _, err := client.Confirm(ctx, attempt, code); err != nil {
    return err
}
result, err := client.Status(ctx, attempt)
```

The `accessToken` callback is `func(context.Context) (string, error)` and returns the tester's current Cognito access token. Inject a transport for integration tests, or pass `nil` to use the default transport. The client sends JSON to the selected HTTPS origin and rejects redirects.

Keep the same attempt and recipient when retrying Start so its operation ID, proof and authorization time stay fixed. Treat `result.Accepted` as completion. Poll with a context deadline and a delay between each request if the host responds with `confirmed` or `ready` and acceptance is still pending. Check terminal states such as `expired` and `attempts_exhausted`. API errors expose HTTP status and a stable code through `*testhost.ResponseError`.

For the full verification service in a composed Go application, use [the backend feature library](https://github.com/lambdawalker/go.apexfission.aws.email-verification). This client drives the isolated demo API.
