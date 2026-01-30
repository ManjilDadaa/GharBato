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
 * Instrumented test for SearchTopBar component in SearchScreen
 * Tests that the SearchTopBar header "Find Your Dream Property" is displayed
 */
@RunWith(AndroidJUnit4::class)
class SearchTopBarInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<DashboardActivity>()

    @Test
    fun searchTopBar_headerText_isDisplayed() {
        // Wait for the UI to load
        composeRule.waitForIdle()

        // Click on Search tab in bottom navigation
        composeRule.onNodeWithText("Search")
            .performClick()

        // Wait for navigation
        composeRule.waitForIdle()

        // Verify that SearchTopBar's header text is displayed
        composeRule.onNodeWithText("Find Your Dream Property")
            .assertIsDisplayed()
    }
}
