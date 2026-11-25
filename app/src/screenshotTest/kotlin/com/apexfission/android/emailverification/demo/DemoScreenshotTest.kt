package com.apexfission.android.emailverification.demo

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.emailverification.EmailVerificationState
import com.apexfission.android.emailverification.EmailVerificationWidget
import com.apexfission.android.emailverification.VerificationMessage
import com.apexfission.android.emailverification.VerificationStep

@Preview(name = "Light", widthDp = 393, heightDp = 800, locale = "en-rUS", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", widthDp = 393, heightDp = 800, locale = "en-rUS", uiMode = Configuration.UI_MODE_NIGHT_YES)
private annotation class VerificationThemes

class DemoScreenshotTest {
    @PreviewTest
    @VerificationThemes
    @Preview(name = "Narrow and large text", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-rGB")
    @Preview(name = "Narrow and large text dark", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-rGB", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun manualCode() = FlowFixture(VerificationStep.Code)

    @PreviewTest
    @VerificationThemes
    @Composable
    fun manualCodeReady() = FlowFixture(VerificationStep.Code, code = DEMO_CODE)

    @PreviewTest
    @VerificationThemes
    @Preview(name = "Narrow and large text", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-rGB")
    @Preview(name = "Spanish", widthDp = 393, heightDp = 800, locale = "es-rMX")
    @Composable
    fun catalog() = DemoTheme { DemoCatalogScreen({}, {}, {}) }

    @PreviewTest
    @VerificationThemes
    @Composable
    fun offlineFlow() = FlowFixture(VerificationStep.Email)

    @PreviewTest
    @VerificationThemes
    @Composable
    fun offlineError() = FlowFixture(VerificationStep.Code, message = VerificationMessage.InvalidCode)

    @PreviewTest
    @VerificationThemes
    @Composable
    fun offlineSuccess() = FlowFixture(VerificationStep.Verified)

    @PreviewTest
    @VerificationThemes
    @Preview(name = "Narrow and large text", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-rGB")
    @Composable
    fun liveSettings() = ConnectionFixture(DemoDirectory())

    @PreviewTest
    @VerificationThemes
    @Composable
    fun directorySettings() = ConnectionFixture(DemoDirectory(entries = listOf(
        DemoEnvironment("dev", DEFAULT_ORIGIN, "us-east-2_demo", "demo-client"),
        DemoEnvironment("qa", "https://qa.components.email-verification.apexfission.com", "us-east-2_qa", "qa-client"),
    )))

    @PreviewTest
    @VerificationThemes
    @Composable
    fun directoryUnavailable() = ConnectionFixture(DemoDirectory(unavailable = true))

    @PreviewTest
    @VerificationThemes
    @Composable
    fun directoryLoading() = ConnectionFixture(DemoDirectory(loading = true))

    @PreviewTest
    @VerificationThemes
    @Composable
    fun liveFlow() = FlowFixture(VerificationStep.Code, live = DEFAULT_ORIGIN)
}

@Composable
private fun ConnectionFixture(directory: DemoDirectory) {
    DemoTheme {
        LiveConnectionScreen(onConnect = { _, _ -> false }, onCancel = {}, directory = directory, onRefreshDirectory = {})
    }
}

@Composable
private fun FlowFixture(step: VerificationStep, live: String? = null, message: VerificationMessage? = null, code: String = "") {
    DemoTheme {
        DemoFlowScreen(live, {}) {
            EmailVerificationWidget(
                EmailVerificationState(step, "tester@example.invalid", code = code, message = message),
                {}, {}, {}, {}, {}, {},
            )
        }
    }
}
