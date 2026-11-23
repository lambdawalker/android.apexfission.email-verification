package com.apexfission.android.emailverification.demo

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.apexfission.android.emailverification.EmailVerificationState
import com.apexfission.android.emailverification.EmailVerificationWidget
import com.apexfission.android.emailverification.VerificationStep

@Preview(name = "Light", widthDp = 393, heightDp = 800, locale = "en-rUS", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", widthDp = 393, heightDp = 800, locale = "en-rUS", uiMode = Configuration.UI_MODE_NIGHT_YES)
private annotation class VerificationThemes

private enum class Screen { Offline, Settings, Live }

class DemoScreenshotTest {
    @PreviewTest
    @VerificationThemes
    @Composable
    fun offlineFlow() = DemoFixture(Screen.Offline)

    @PreviewTest
    @VerificationThemes
    @Preview(name = "Narrow and large text", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-rGB")
    @Composable
    fun liveSettings() = DemoFixture(Screen.Settings)

    @PreviewTest
    @VerificationThemes
    @Composable
    fun liveFlow() = DemoFixture(Screen.Live)
}

@Composable
private fun DemoFixture(screen: Screen) {
    DemoTheme {
        DemoFrame {
            when (screen) {
                Screen.Settings -> LiveConnectionForm({ _, _ -> false }, {})
                Screen.Offline, Screen.Live -> {
                    val live = screen == Screen.Live
                    DemoMode(if (live) DEFAULT_ORIGIN else null, {})
                    val state = EmailVerificationState(
                        if (live) VerificationStep.Code else VerificationStep.Email,
                        "tester@example.invalid",
                    )
                    EmailVerificationWidget(state, {}, {}, {}, {}, {}, {})
                }
            }
        }
    }
}
