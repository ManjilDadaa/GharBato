package com.example.gharbato

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.view.SearchTopBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchTopBarInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun filterActionSitsAtTheTrailingEdgeAndSearchStillSubmitsAndClears() {
        var searches = 0
        var filtersOpened = 0
        var queryAtSearch = ""
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                GharBatoTheme {
                    var query by remember { mutableStateOf("") }
                    Box(Modifier.width(360.dp)) {
                        SearchTopBar(
                            searchQuery = query,
                            onSearchQueryChange = { query = it },
                            onSearchClick = { searches++; queryAtSearch = query },
                            onClearSearch = { query = "" },
                            onSearchBarFocused = {},
                            hasActiveSearch = query.isNotEmpty(),
                            onFilterClick = { filtersOpened++ },
                            activeFilterCount = 2
                        )
                    }
                }
            }
        }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("Patan")
        field.performImeAction()
        compose.runOnIdle { assertEquals(1, searches); assertEquals("Patan", queryAtSearch) }

        val filter = compose.onNodeWithContentDescription("Filters (2)")
        filter.assertIsDisplayed().assertHasClickAction()
        val fieldBounds = field.getUnclippedBoundsInRoot()
        val filterBounds = filter.getUnclippedBoundsInRoot()
        assertTrue(filterBounds.left > fieldBounds.left + (fieldBounds.right - fieldBounds.left) / 2)
        assertTrue(filterBounds.right <= fieldBounds.right)
        assertTrue(filterBounds.top >= fieldBounds.top && filterBounds.bottom <= fieldBounds.bottom)
        filter.performClick()
        compose.runOnIdle { assertEquals(1, filtersOpened) }

        compose.onNodeWithContentDescription("Clear search").performClick()
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithContentDescription("Clear search").assertDoesNotExist()
        filter.assertIsDisplayed()
    }
}
