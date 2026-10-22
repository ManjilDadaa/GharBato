package com.example.gharbato

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.view.DetailsContentScreen
import com.example.gharbato.view.FurnishingDropdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ListingControlsInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun furnishingFitsNarrowPhoneAndEveryChoiceCanBeSelected() {
        verifyFurnishing(fontScale = 1f, darkTheme = false, captureName = "furnishing-narrow")
    }

    @Test fun furnishingStaysOnOneLineWithLargeFonts() {
        verifyFurnishing(fontScale = 1.5f, darkTheme = true, captureName = "furnishing-large-font")
    }

    @Test fun listingDetailsGiveFurnishingEnoughWidthOnNarrowPhones() {
        compose.setContent {
            GharBatoTheme(darkTheme = false) {
                var state by remember {
                    mutableStateOf(PropertyListingState(selectedPurpose = "Rent", selectedPropertyType = "Apartment", furnishing = "Semi Furnished"))
                }
                Box(Modifier.width(320.dp).padding(16.dp)) {
                    DetailsContentScreen(state, onStateChange = { state = it })
                }
            }
        }
        compose.onNodeWithText("Furnishing").performScrollTo().assertWidthIsAtLeast(250.dp)
        assertSingleLine(compose.onNodeWithText("Semi Furnished", useUnmergedTree = true))
        capture(compose.onRoot(), "listing-details-narrow-furnishing")
    }

    private fun verifyFurnishing(fontScale: Float, darkTheme: Boolean, captureName: String) {
        var selected = "Semi Furnished"
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                GharBatoTheme(darkTheme = darkTheme) {
                    var furnishing by remember { mutableStateOf(selected) }
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        Box(Modifier.width(320.dp).padding(16.dp)) {
                            FurnishingDropdown(
                                selectedFurnishing = furnishing,
                                onFurnishingChange = { furnishing = it; selected = it },
                                modifier = Modifier.fillMaxWidth(),
                                isDarkMode = darkTheme
                            )
                        }
                    }
                }
            }
        }

        assertSingleLine(compose.onNodeWithText(selected, useUnmergedTree = true))
        capture(compose.onRoot(), captureName)

        listOf("Fully Furnished", "Unfurnished", "Semi Furnished").forEachIndexed { index, option ->
            compose.onNodeWithText("Furnishing").performClick()
            if (index == 0) capture(compose.onNode(isPopup()), "$captureName-menu")
            listOf("Fully Furnished", "Semi Furnished", "Unfurnished").forEach { choice ->
                val item = compose.onNode(hasText(choice) and hasAnyAncestor(isPopup()), useUnmergedTree = true)
                assertSingleLine(item)
            }
            compose.onNode(hasText(option) and hasAnyAncestor(isPopup())).performClick()
            compose.runOnIdle { assertEquals(option, selected) }
            assertSingleLine(compose.onNodeWithText(option, useUnmergedTree = true))
        }
    }

    private fun assertSingleLine(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayouts ->
            assertTrue(getLayouts(layouts))
        }
        assertTrue("A visible text layout is required", layouts.isNotEmpty())
        val bounds = node.fetchSemanticsNode().boundsInRoot
        layouts.forEach { layout ->
            val diagnostics = "text='${layout.layoutInput.text}', nodeBounds=$bounds, " +
                "layoutSize=${layout.size}, constraints=${layout.layoutInput.constraints}, " +
                "lines=${layout.lineCount}, widthOverflow=${layout.didOverflowWidth}, " +
                "heightOverflow=${layout.didOverflowHeight}, ellipsized=${layout.isLineEllipsized(0)}, " +
                "lineBounds=${layout.getLineLeft(0)},${layout.getLineTop(0)},${layout.getLineRight(0)},${layout.getLineBottom(0)}"
            assertEquals("Furnishing labels must stay on one line: $diagnostics", 1, layout.lineCount)
            // TextLayoutResult can report width overflow when its paragraph is wider than
            // its intrinsic-sized node even though every glyph fits. Check the drawn line.
            assertTrue("Furnishing text must fit its visible width: $diagnostics",
                layout.getLineLeft(0) >= -1f &&
                    layout.getLineRight(0) <= minOf(layout.size.width.toFloat(), bounds.width) + 1f)
            assertFalse("Furnishing text must fit its visible height: $diagnostics", layout.didOverflowHeight)
            assertTrue("Furnishing text must fit its line height: $diagnostics",
                layout.getLineTop(0) >= -1f && layout.getLineBottom(0) <= layout.size.height + 1f)
            assertFalse("Furnishing labels must not be truncated: $diagnostics", layout.isLineEllipsized(0))
        }
    }

    private fun capture(node: SemanticsNodeInteraction, name: String) {
        val directory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "ui-review"
        ).apply { mkdirs() }
        node.captureToImage().asAndroidBitmap().let { bitmap ->
            File(directory, "$name.png").outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }
}
