package com.apexfission.android.emailverification.demo

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.apexfission.android.emailverification.R as WidgetR
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DemoNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsHeaderOpensTheInstalledPackage() {
        Intents.init()
        try {
            val expected = allOf(
                hasAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
                hasData(Uri.fromParts("package", compose.activity.packageName, null)),
            )
            Intents.intending(expected).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
            compose.onNodeWithText(compose.activity.getString(R.string.app_settings)).assertIsDisplayed().performClick()
            Intents.intended(expected)
            compose.onNodeWithText(compose.activity.getString(R.string.catalog_title)).assertIsDisplayed()
        } finally {
            Intents.release()
        }
    }

    @Test fun systemSettingsReturnsToAUsableCatalog() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val packageName = compose.activity.packageName
        compose.onNodeWithText(compose.activity.getString(R.string.app_settings)).performClick()
        assertTrue("Android Settings opened", device.wait(Until.hasObject(By.pkg("com.android.settings").depth(0)), 10_000))
        device.pressBack()
        assertTrue("Demo returned", device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 10_000))
        compose.onNodeWithText(compose.activity.getString(R.string.open_offline)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(WidgetR.string.ev_send_code)).performScrollTo().assertIsDisplayed()
    }

    @Test fun systemBackDiscardsTheAttemptAndMenuCanStartAgain() {
        compose.onNodeWithText(compose.activity.getString(R.string.open_offline)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(WidgetR.string.ev_send_code)).performScrollTo().performClick()
        compose.onAllNodesWithText(compose.activity.getString(WidgetR.string.ev_check_email))[0].performScrollTo().assertIsDisplayed()
        pressBack()
        compose.onNodeWithText(compose.activity.getString(R.string.app_settings)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.open_offline)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(WidgetR.string.ev_send_code)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back_to_menu)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.app_settings)).assertIsDisplayed()
    }

    @Test fun activityRecreationRetainsTheActiveOfflineFlow() {
        compose.onNodeWithText(compose.activity.getString(R.string.open_offline)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(WidgetR.string.ev_send_code)).performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.onAllNodesWithText(compose.activity.getString(WidgetR.string.ev_check_email))[0].performScrollTo().assertIsDisplayed()
        pressBack()
        compose.onNodeWithText(compose.activity.getString(R.string.app_settings)).assertIsDisplayed()
    }

    @Test fun manualCodeRetainsValidationRecoveryAndExplicitCompletion() {
        compose.onNodeWithText(compose.activity.getString(R.string.open_offline)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(WidgetR.string.ev_send_code)).performScrollTo().performClick()
        val confirm = compose.onNode(hasText(compose.activity.getString(WidgetR.string.ev_verify)) and hasClickAction())
        confirm.performScrollTo().assertIsNotEnabled()
        val field = compose.onNodeWithContentDescription(compose.activity.getString(WidgetR.string.ev_six_digit_code))
        field.performScrollTo().performTextInput("000000")
        confirm.performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText(compose.activity.getString(WidgetR.string.ev_invalid_code)).performScrollTo().assertIsDisplayed()
        field.performScrollTo().performTextReplacement(DEMO_CODE)
        // Entering all digits never submits by itself.
        compose.onNodeWithText(compose.activity.getString(WidgetR.string.ev_awaiting_confirmation)).performScrollTo().assertIsDisplayed()
        confirm.performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(WidgetR.string.ev_verified)).performScrollTo().assertIsDisplayed()
    }
}
