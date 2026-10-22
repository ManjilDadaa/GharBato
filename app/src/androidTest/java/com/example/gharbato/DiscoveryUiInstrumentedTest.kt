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
import androidx.compose.ui.semantics.SemanticsActions
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

    @Test fun cardOpensDetailsOnTapAndOptionsOnLongPressWithoutOverlayButtons() {
        var shares = 0
        var detailClicks = 0
        var reports = 0
        var hides = 0
        compose.setContent {
            GharBatoTheme(darkTheme = false) {
                PropertyListingCard(property, onClick = { detailClicks++ },
                    onShareClick = { shares++ }, onReportClick = { reports++ }, onHideClick = { hides++ }, modifier = Modifier.padding(16.dp))
            }
        }
        compose.onNodeWithText(property.title).assertIsDisplayed()
        compose.onNodeWithText("Rs 25,500/month").assertIsDisplayed()
        compose.onNodeWithContentDescription("Save property").assertDoesNotExist()
        compose.onNodeWithContentDescription("Remove from saved").assertDoesNotExist()
        compose.onNodeWithContentDescription("Property options").assertDoesNotExist()
        capture("property-card-light")
        compose.onNodeWithText(property.title).performTouchInput { longClick() }
        compose.onNodeWithText("Report listing").assertIsDisplayed()
        compose.onNodeWithText("Not interested").assertIsDisplayed()
        compose.onNodeWithText("Share property").performClick()
        compose.runOnIdle { assertEquals(1, shares); assertEquals(0, detailClicks) }
        compose.onNodeWithText(property.title).performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        compose.onNodeWithText("Report listing").performClick()
        compose.onNodeWithText(property.title).performTouchInput { longClick() }
        compose.onNodeWithText("Not interested").performClick()
        compose.onNodeWithText(property.title).performClick()
        compose.runOnIdle {
            assertEquals(1, reports)
            assertEquals(1, hides)
            assertEquals(1, detailClicks)
        }
    }

    @Test fun homeSearchAndMarketChipsWorkWithoutAListingShortcut() {
        var searched = ""
        var selectedMarketType by mutableStateOf("")
        compose.setContent {
            GharBatoTheme(darkTheme = false) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
                    HomeHeader(0, {}, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground)
                    HomeSearchSection(onSearch = { searched = it })
                    QuickActionsSection(selectedMarketType) { selectedMarketType = it }
                    PropertyListingCard(property, {}, modifier = Modifier.padding(horizontal = com.example.gharbato.ui.theme.GharBatoSpacing.screenHorizontal, vertical = 16.dp))
                }
            }
        }
        capture("home-discovery")
        compose.onNodeWithText("Search city, area, or property").performTextInput("Patan")
        compose.onNodeWithText("Patan").performImeAction()
        compose.runOnIdle { assertEquals("Patan", searched) }
        compose.onNodeWithText("Find your dream property").assertIsDisplayed()
        compose.onNodeWithText("List your property").assertDoesNotExist()
        compose.onNodeWithText("Rent").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals("Rent", selectedMarketType) }
        compose.onNodeWithText("Buy").performClick().assertIsSelected()
        compose.onNodeWithText("Rent").assertIsNotSelected()
        compose.runOnIdle { assertEquals("Sell", selectedMarketType) }
        compose.onNodeWithText("Stay").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals("Book", selectedMarketType) }
        compose.onNodeWithText("Stay").performClick().assertIsNotSelected()
        compose.runOnIdle { assertEquals("", selectedMarketType) }
    }

    @Test fun searchModeAndFilterControlsWorkAtLargeFontSizes() {
        var filtersOpened = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                GharBatoTheme(darkTheme = false) {
                    var map by remember { mutableStateOf(false) }
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        SearchTopBar("", {}, {}, {}, {}, false, { filtersOpened = true }, 2)
                        SortBar(24, SortOption.DATE_NEWEST, {}, {}, map, { map = !map })
                    }
                }
            }
        }
        compose.onNodeWithText("List").assertIsSelected()
        compose.onNodeWithText("Map").performClick().assertIsSelected()
        compose.onNodeWithText("List").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Filters (2)").performClick()
        compose.runOnIdle { assertTrue(filtersOpened) }
        capture("search-controls-large-font")
    }

    @Test fun darkCardsKeepReadableFactsAndActions() {
        compose.setContent {
            GharBatoTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                    PropertyListingCard(property.copy(isFavorite = true), {})
                }
            }
        }
        compose.onNodeWithText("2 beds").assertIsDisplayed()
        compose.onNodeWithText("1 bath").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove from saved").assertDoesNotExist()
        compose.onNodeWithText(property.title).assertHasClickAction()
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
