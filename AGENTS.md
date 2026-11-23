# Repository guidance

- Read [the consumer guides](docs/agents/index.md) for the public boundaries between Android, the demo and Go.
- Keep the demo backend connection optional. Never commit a live recipient, access token, secret proof, or credentials.
- Verify Android changes with `./gradlew :email-verification:testDebugUnitTest :email-verification:lintDebug :app:lintDebug :app:assembleDebug` and the [screenshot suite](docs/agents/screenshots.md).
- Verify Go changes with `go test -race ./...` and `go vet ./...`.
- Run `sh ./docs.sh check` and `sh ./docs.sh build --check` after changing canonical guides. Keep site output and CLI caches out of Git.
- Refresh the module inventory with the configured `android-repo-setup` release and `android-repo-setup discover --save`. Publishing workflows are introduced when the first release is planned.
