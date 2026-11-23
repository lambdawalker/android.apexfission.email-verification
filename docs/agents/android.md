# Integrate on Android

## Source installation

The library targets API 28+ and uses Java 17 bytecode and Material 3. The reference build uses Android SDK 37.0. Once the source is in the consumer checkout, include `:email-verification` in `settings.gradle.kts` and use `implementation(project(":email-verification"))`. Keep the version catalog entries used by that module and configure Google Maven, Maven Central and JitPack as in this repo's settings. For an existing multi-repository build, use a local Gradle checkout or package an AAR with the module's dependencies. This repository is in development and has source installation only.

## Render in your host

Inside a Material 3 screen use `EmailVerificationWidget(controller, modifier)`. Give it a scrollable parent when it has limited space. It uses the host's color scheme, typography and shapes. The same widget has a controlled overload that accepts `EmailVerificationState` and actions for projects with their own state machine.

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
