package com.example.gharbato

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gharbato.model.PropertyFilters
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.SortOption
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.view.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DiscoveryUiInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val property = PropertyModel(id = 1, title = "Bright two-bedroom apartment", price = "25500", marketType = "Rent", location = "Bhaisepati, Lalitpur", bedrooms = 2, bathrooms = 1, sqft = "1,100 sq.ft")

    @Test fun cardShowsItsTitleBudgetAndDiscoverableActions() {
        var shares = 0
        compose.setContent {
            var saved by remember { mutableStateOf(false) }
            GharBatoTheme(darkTheme = false) {
                PropertyListingCard(property.copy(isFavorite = saved), onClick = {}, onFavoriteClick = { saved = !saved },
                    onShareClick = { shares++ }, onReportClick = {}, onHideClick = {}, modifier = Modifier.padding(16.dp))
            }
        }
        compose.onNodeWithText(property.title).assertIsDisplayed()
        compose.onNodeWithText("Rs 25,500/month").assertIsDisplayed()
        compose.onNodeWithContentDescription("Save property").performClick()
        compose.onNodeWithContentDescription("Remove from saved").assertIsToggleable().assertIsOn()
        capture("property-card-light")
        compose.onNodeWithContentDescription("Property options").performClick()
        compose.onNodeWithText("Report listing").assertIsDisplayed()
        compose.onNodeWithText("Not interested").assertIsDisplayed()
        compose.onNodeWithText("Share property").performClick()
        compose.runOnIdle { assertEquals(1, shares) }
    }

    @Test fun homeSearchAndListingEntryWorkWithoutWaitingForProperties() {
        var searched = ""
        var listingOpened = false
        compose.setContent {
            GharBatoTheme(darkTheme = false) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
                    HomeHeader(0, {}, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground)
                    HomeSearchSection(onSearch = { searched = it }, onListProperty = { listingOpened = true })
                    QuickActionsSection({}, {}, {})
                    PropertyListingCard(property, {}, onFavoriteClick = {}, modifier = Modifier.padding(horizontal = com.example.gharbato.ui.theme.GharBatoSpacing.screenHorizontal, vertical = 16.dp))
                }
            }
        }
        capture("home-discovery")
        compose.onNodeWithText("Search city, area, or property").performTextInput("Patan")
        compose.onNodeWithText("Patan").performImeAction()
        compose.runOnIdle { assertEquals("Patan", searched) }
        compose.onNodeWithText("List your property").performClick()
        compose.runOnIdle { assertTrue(listingOpened) }
    }

    @Test fun searchModeAndFilterControlsWorkAtLargeFontSizes() {
        var filtersOpened = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                GharBatoTheme(darkTheme = false) {
                    var map by remember { mutableStateOf(false) }
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        SearchTopBar("", {}, {}, {}, {}, {}, false, false)
                        SortBar(24, SortOption.DATE_NEWEST, {}, { filtersOpened = true }, false, map, { map = !map }, 2)
                    }
                }
            }
        }
        compose.onNodeWithText("List").assertIsSelected()
        compose.onNodeWithText("Map").performClick().assertIsSelected()
        compose.onNodeWithText("List").performClick().assertIsSelected()
        compose.onNodeWithText("Filters (2)").performClick()
        compose.runOnIdle { assertTrue(filtersOpened) }
        capture("search-controls-large-font")
    }

    @Test fun darkCardsKeepReadableFactsAndActions() {
        compose.setContent {
            GharBatoTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                    PropertyListingCard(property.copy(isFavorite = true), {}, onFavoriteClick = {})
                }
            }
        }
        compose.onNodeWithText("2 beds").assertIsDisplayed()
        compose.onNodeWithText("1 bath").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove from saved").assertIsDisplayed()
        capture("property-card-dark")
    }

    @Test fun priceFiltersApplyFullNprAmountsAndRejectReversedRanges() {
        var applied: PropertyFilters? = null
        compose.setContent {
            GharBatoTheme(darkTheme = false) {
                FilterBottomSheet(PropertyFilters(), { applied = it }, {}, previewResultsCount = { 4 })
            }
        }
        compose.onNodeWithText("Minimum (NPR)").performScrollTo().performTextInput("25500")
        compose.onNodeWithText("Maximum (NPR)").performTextInput("10000")
        compose.onNodeWithText("Show 4 properties").assertIsNotEnabled()
        compose.onNodeWithText("Maximum (NPR)").performTextReplacement("50000")
        compose.onNodeWithText("Show 4 properties").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(25500, applied!!.minPrice)
            assertEquals(50000, applied!!.maxPrice)
        }
    }

    @Test fun mapPreviewKeepsTheTitleAndFactsVisibleWithLargeText() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                GharBatoTheme(darkTheme = true) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
                        PropertyDetailOverlay(property, {}, {}, Modifier.padding(16.dp), true)
                    }
                }
            }
        }
        compose.onNodeWithText(property.title).assertIsDisplayed()
        compose.onNodeWithText("2 beds").assertIsDisplayed()
        compose.onNodeWithText("View property").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close property preview").assertHasClickAction()
        capture("map-preview-large-font")
    }

    @Test fun contactBarShowsBothActionsAtLargeTextSizes() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                GharBatoTheme(darkTheme = true) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        BottomActionButtons(property, MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        compose.onNodeWithText("Call").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Message owner").assertIsDisplayed().assertHasClickAction()
        capture("contact-actions-dark-large-font")
    }

    private fun capture(name: String) {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "ui-review").apply { mkdirs() }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
