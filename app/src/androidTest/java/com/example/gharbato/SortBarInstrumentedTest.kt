package com.example.gharbato

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gharbato.model.SortOption
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.view.SortBar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SortBarInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactResultsControlsKeepSortLocationAndMapActions() {
        var sortClicks = 0
        var locationClicks = 0
        compose.setContent {
            GharBatoTheme {
                var showMap by remember { mutableStateOf(false) }
                SortBar(
                    propertiesCount = 24,
                    currentSort = SortOption.DATE_NEWEST,
                    onSortClick = { sortClicks++ },
                    onLocationClick = { locationClicks++ },
                    showMap = showMap,
                    onMapToggle = { showMap = !showMap }
                )
            }
        }
        compose.onNodeWithText("24 properties").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sort properties:", substring = true).performClick()
        compose.onNodeWithContentDescription("Search near me").performClick()
        compose.runOnIdle { assertEquals(1, sortClicks); assertEquals(1, locationClicks) }
        compose.onNodeWithText("List").assertIsSelected()
        compose.onNodeWithText("Map").performClick().assertIsSelected()
        compose.onNodeWithText("List").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Filters").assertDoesNotExist()
    }
}
