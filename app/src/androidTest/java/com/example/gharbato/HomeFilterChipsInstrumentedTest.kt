package com.example.gharbato

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.view.QuickActionsSection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeFilterChipsInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun iconsAndLabelsStayCenteredWhenChipsAreSelectedOrCleared() {
        compose.setContent {
            GharBatoTheme {
                var selectedMarketType by remember { mutableStateOf("") }
                Box(Modifier.width(360.dp)) {
                    QuickActionsSection(selectedMarketType, { selectedMarketType = it })
                }
            }
        }

        assertChipContentsCentered("Rent", "Rent")
        assertChipContentsCentered("Buy", "Sell")
        assertChipContentsCentered("Stay", "Book")
        compose.onNodeWithText("Rent").performClick().assertIsSelected()
        assertChipContentsCentered("Rent", "Rent")
        compose.onNodeWithText("Buy").performClick().assertIsSelected()
        compose.onNodeWithText("Rent").assertIsNotSelected()
        assertChipContentsCentered("Buy", "Sell")
        compose.onNodeWithText("Stay").performClick().assertIsSelected()
        assertChipContentsCentered("Stay", "Book")
        compose.onNodeWithText("Stay").performClick().assertIsNotSelected()
        assertChipContentsCentered("Stay", "Book")
    }

    private fun assertChipContentsCentered(title: String, marketType: String) {
        val chip = compose.onNodeWithText(title).fetchSemanticsNode().boundsInRoot
        val label = compose.onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val icon = compose.onNodeWithTag("home_filter_icon_$marketType", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val contentCenterX = (minOf(label.left, icon.left) + maxOf(label.right, icon.right)) / 2f
        val contentCenterY = (minOf(label.top, icon.top) + maxOf(label.bottom, icon.bottom)) / 2f
        assertEquals("$title contents should be horizontally centered", chip.center.x, contentCenterX, 2f)
        assertEquals("$title contents should be vertically centered", chip.center.y, contentCenterY, 2f)
    }
}
