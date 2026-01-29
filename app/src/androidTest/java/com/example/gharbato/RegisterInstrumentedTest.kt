package com.example.gharbato

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.example.gharbato.view.DashboardActivity
import com.example.gharbato.view.LoginActivity
import com.example.gharbato.view.SignUpActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RegisterInstrumentedTest {
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
    fun test_dont_have_account_to_dashboard() {
        // Passing test
        composeRule.onNodeWithTag("create_account")
            .performClick()
        Intents.intended(hasComponent(SignUpActivity::class.java.name))
    }
}