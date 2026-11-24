# Run the demo

## Offline

Run the `app` module from Android Studio or use `./gradlew :app:installDebug`. Open **Email Verification Demo**, leave the sample address in place, tap **Send code**, then enter `271828`. Enter a different six-digit code first to see the error and retry state. Offline mode uses an in-memory fixture and sends no email.

## Live

The app loads the public directory at
`https://index.components.email-verification.apexfission.com/v1/environments`.
Choose an environment to fill in its API URL and show its Cognito public client
settings. If discovery is unavailable or a custom host is in use, enter the
API URL manually. The access token is used only for the test host.

1. Deploy the [Go email-verification test host](https://github.com/lambdawalker/go.apexfission.aws.email-verification/blob/main/docs/design/test-host.md) with a mobile hostname. Use the deployed `testHostMobileURL` for the chosen environment. The suggested development hostname is `https://dev.components.email-verification.apexfission.com`.
2. Verify the SES sender and configure the single permitted test recipient. If SES is in its sandbox, verify the recipient in SES too. Invite a tester in the environment's Cognito pool and complete any first-login challenge in an SRP-capable client using `testHostMobileUserPoolID` and `testHostMobileClientID`.
3. Sign in to the same pool and app client via SRP and obtain the session's Cognito **access token**. For example, an existing Amplify client can retrieve it with `(await fetchAuthSession()).tokens?.accessToken?.toString()` in JavaScript. In the demo, choose **Connect to test host**, enter the selected URL and paste the access token. The app keeps it in memory. Clear any clipboard copy after pasting.
4. Send to the configured test recipient and enter the code you received. The result may take a worker cycle to settle. Use **Check status** if automatic polling ends before the host accepts it.

The demo consumes a developer-supplied session. Use a fresh access token if authorization expires. Switching back to offline mode discards the live session and its proof. A 401 or 403 can indicate an expired token or a pool or app client mismatch. A code request rejected on submit can indicate that the address differs from the configured recipient. For the deployment and mail settings, see the test-host runbook above.
