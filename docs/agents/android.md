# Integrate on Android

## Source installation

The library targets API 28+ and uses Java 17 bytecode and Material 3. The reference build uses Android SDK 37.0. Once the source is in the consumer checkout, include `:email-verification` in `settings.gradle.kts` and use `implementation(project(":email-verification"))`. Keep the version catalog entries used by that module and configure Google Maven, Maven Central and JitPack as in this repo's settings. For an existing multi-repository build, use a local Gradle checkout or package an AAR with the module's dependencies. This repository is in development and has source installation only.

## Render in your host

Inside a Material 3 screen use `EmailVerificationWidget(controller, modifier)`. Give it a scrollable parent when it has limited space. It uses the host's color scheme, typography and shapes. The same widget has a controlled overload that accepts `EmailVerificationState` and actions for projects with their own state machine.

The manual-code screen uses UI Common's detailed verification card: six rounded cells, a label
and length legend, explanatory copy, and a recipient/status summary. Its primary action remains
disabled until six digits are entered and requires an explicit tap. The card says **Awaiting
confirmation** until the controller advances; entering a code does not imply that a link opened.
Paste code and Change destination are text buttons below the recipient card. Paste reads the
clipboard only after a tap, and Change destination uses the existing start-over callback.
Tapping a populated digit selects it for replacement; the border and fill follow the selection
as the user types. Check status retains its existing callback. Resend, host onboarding
progress, product branding and session-isolation claims are not inferred by this component.
The demo owns the reference-inspired blue/slate palette. The library always inherits its host.

## UI Common dependency

The shared card and code editor come from the confirmed **UI Common 0.1.1** JitPack release:

```kotlin
implementation("com.github.lambdawalker:android.ui.common:ui-common~v0.1.1-jitpack")
```

The version catalog pins this artifact for both the library and the demo. Normal Gradle builds
and CI resolve it directly from JitPack. The temporary source checkout and composite substitution
have been removed; no extra property or local UI Common repository is needed.

[Upstream installation contract](https://github.com/lambdawalker/android.ui.common/blob/main/IMPORT.md).
The release tag points to [the merged implementation](https://github.com/lambdawalker/android.ui.common/commit/84a2dc6586c63350d9d8867f6f7e6d3d9f0309e6).

## Controller ownership

Create one `EmailVerificationController` per flow with the screen ViewModel's `viewModelScope`. Start with an injected `VerificationGateway` or with `TestHostGateway(origin) { signedInAccessToken() }`. Hosts that provide an HTTP gateway should add the manifest `INTERNET` permission. The widget itself needs no runtime permissions.

```kotlin
class DemoVerificationModel(gateway: VerificationGateway) : ViewModel() {
    val controller = EmailVerificationController(gateway, viewModelScope)
}

@Composable
fun VerifyEmail(
    model: DemoVerificationModel,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by model.controller.state.collectAsStateWithLifecycle()
    val complete by rememberUpdatedState(onVerified)
    LaunchedEffect(state.step) {
        if (state.step == VerificationStep.Verified) complete()
    }

    Column(modifier.verticalScroll(rememberScrollState())) {
        EmailVerificationWidget(model.controller)
    }
}
```

The host should consume completion idempotently or remove this screen after advancing. `Verified` means the host accepted the result. Keep the attempt proof in memory across configuration changes and discard it with the screen's scope. Process death starts a fresh flow. The current test-host gateway supports email codes and status polling. Link handling, resend policy and app sign-in belong to the integrating host. For another API, supply a gateway with its own protocol implementation and map responses to the public widget states and failure types. Surface recoverable gateway errors as `VerificationRequestException`.
