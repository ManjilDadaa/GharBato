package com.example.gharbato

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gharbato.view.DashboardActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for SortBar component in SearchScreen
 * Tests that the SortBar displays "Properties available" subtitle text
 */
@RunWith(AndroidJUnit4::class)
class SortBarInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<DashboardActivity>()

    @Test
    fun sortBar_propertiesAvailableText_isDisplayed() {
        // Wait for the UI to load
        composeRule.waitForIdle()

        // Click on Search tab in bottom navigation
        composeRule.onNodeWithText("Search")
            .performClick()

        // Wait for navigation and data loading
        composeRule.waitForIdle()
        Thread.sleep(2000)

        // Verify that SortBar's "Properties available" subtitle is displayed
        composeRule.onNodeWithText("Properties available")
            .assertIsDisplayed()
    }
}
