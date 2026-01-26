package com.example.gharbato

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gharbato.repository.UserRepo
import com.example.gharbato.view.DashboardActivity
import com.example.gharbato.view.LoginActivity
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)

class LoginInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<LoginActivity>()

    @Before
    fun setup(){
        Intents.init()
    }

    @After
    fun tearDown(){
        Intents.release()
    }

    @Test
    fun test_login_to_dashboard() {
        // Enter email and password
        // Failing test
        composeRule.onNodeWithTag("email_input")
            .performTextInput("manjilbasnet@gmail.com")
        composeRule.onNodeWithTag("password_input")
            .performTextInput("1233333")
        composeRule.onNodeWithTag("login_button")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 10000) {
            try {
                Intents.intended(hasComponent(DashboardActivity::class.java.name))
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
}