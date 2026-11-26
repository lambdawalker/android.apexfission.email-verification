package com.apexfission.android.emailverification

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.tools.screenshot.PreviewTest

@Preview(name = "Light", widthDp = 393, heightDp = 800, locale = "en-rUS", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", widthDp = 393, heightDp = 800, locale = "en-rUS", uiMode = Configuration.UI_MODE_NIGHT_YES)
private annotation class VerificationThemes

private const val email = "tester@example.invalid"

class WidgetScreenshotTest {
    @PreviewTest
    @VerificationThemes
    @Preview(name = "Narrow and large text", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-rGB")
    @Composable
    fun emailEntry() = WidgetFixture(EmailVerificationState())

    @PreviewTest
    @VerificationThemes
    @Composable
    fun sending() = WidgetFixture(EmailVerificationState(email = email, busy = true))

    @PreviewTest
    @VerificationThemes
    @Preview(name = "Narrow and large text", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-rGB")
    @Preview(name = "Narrow and large text dark", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-rGB", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun codeEntry() = WidgetFixture(EmailVerificationState(VerificationStep.Code, email))

    @PreviewTest
    @VerificationThemes
    @Composable
    fun codeBusy() = WidgetFixture(EmailVerificationState(VerificationStep.Code, email, "123456", busy = true))

    @PreviewTest
    @VerificationThemes
    @Preview(name = "Narrow and large text", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-rGB")
    @Preview(name = "Narrow and large text dark", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-rGB", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun codeError() = WidgetFixture(EmailVerificationState(VerificationStep.Code, email, "000000", message = VerificationMessage.InvalidCode))

    @PreviewTest
    @VerificationThemes
    @Composable
    fun codeUnavailable() = WidgetFixture(EmailVerificationState(VerificationStep.Code, email, "123456", message = VerificationMessage.Unavailable))

    @PreviewTest
    @VerificationThemes
    @Composable
    fun waiting() = WidgetFixture(EmailVerificationState(VerificationStep.WaitingForHost, email, busy = true))

    @PreviewTest
    @VerificationThemes
    @Composable
    fun success() = WidgetFixture(EmailVerificationState(VerificationStep.Verified, email))

    @PreviewTest
    @VerificationThemes
    @Composable
    fun expired() = WidgetFixture(EmailVerificationState(VerificationStep.Stopped, email, message = VerificationMessage.Expired))
}

@Composable
private fun WidgetFixture(state: EmailVerificationState) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF81DCC6))
            else lightColorScheme(primary = Color(0xFF006B58)),
        shapes = Shapes(small = RoundedCornerShape(4.dp)),
        typography = Typography(headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 24.sp)),
    ) {
        Surface {
            EmailVerificationWidget(state, {}, {}, {}, {}, {}, {}, Modifier.verticalScroll(rememberScrollState()).padding(16.dp))
        }
    }
}
