package com.example.gharbato

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gharbato.view.ForgotActivity
import com.example.gharbato.view.LoginActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForgotpasswordInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ForgotActivity>()

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun test_forgot_email_verification() {

        // Wait until Compose UI is fully ready
        composeRule.waitForIdle()

        // Enter email in OutlinedTextField
        composeRule
            .onNodeWithTag("email_input", useUnmergedTree = true)
            .performTextInput("aryanshrestha0307@gmail.com")

        // Click Send Reset Link button
        composeRule
            .onNodeWithTag("Send_Reset_Link_Button", useUnmergedTree = true)
            .performClick()

        // Verify navigation to LoginActivity
        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                Intents.intended(hasComponent(LoginActivity::class.java.name))
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
}
