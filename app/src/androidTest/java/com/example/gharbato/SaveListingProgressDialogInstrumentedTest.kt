package com.example.gharbato

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.view.SaveListingProgressDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SaveListingProgressDialogInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun eachActionCallsItsMatchingCallbackAndBackKeepsEditing() {
        var saves = 0
        var exits = 0
        var edits = 0
        compose.setContent {
            GharBatoTheme(darkTheme = false) {
                SaveListingProgressDialog(false, { saves++ }, { exits++ }, { edits++ })
            }
        }
        compose.onNodeWithText("Save and exit").assertIsEnabled().performClick()
        compose.onNodeWithText("Keep editing").assertIsEnabled().performClick()
        compose.onNodeWithText("Exit without saving").assertIsEnabled().performClick()
        capture("save-listing-progress-light")
        Espresso.pressBack()
        compose.runOnIdle {
            assertEquals(1, saves)
            assertEquals(1, exits)
            assertEquals(2, edits)
        }
    }

    @Test fun savingDisablesEveryActionAndPreventsBackDismissal() {
        var callbacks = 0
        compose.setContent {
            GharBatoTheme(darkTheme = true) {
                SaveListingProgressDialog(true, { callbacks++ }, { callbacks++ }, { callbacks++ })
            }
        }
        compose.onNodeWithText("Saving…").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Keep editing").assertIsNotEnabled()
        compose.onNodeWithText("Exit without saving").assertIsNotEnabled()
        compose.onNodeWithText("Save and exit").assertDoesNotExist()
        Espresso.pressBack()
        compose.onNodeWithText("Save your progress?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, callbacks) }
        capture("save-listing-progress-saving-dark")
    }

    @Test fun largeTextAndSaveErrorRemainReadableInLightMode() {
        verifyErrorWithLargeText(darkTheme = false)
    }

    @Test fun largeTextAndSaveErrorRemainReadableInDarkMode() {
        verifyErrorWithLargeText(darkTheme = true)
    }

    private fun verifyErrorWithLargeText(darkTheme: Boolean) {
        val message = "Couldn’t save your draft. Please try again."
        var saves = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                GharBatoTheme(darkTheme = darkTheme) {
                    SaveListingProgressDialog(false, { saves++ }, {}, {}, saveError = message)
                }
            }
        }
        capture("save-listing-progress-error-large-${if (darkTheme) "dark" else "light"}")
        listOf("Save your progress?", message, "Save and exit", "Keep editing", "Exit without saving").forEach { text ->
            val node = compose.onNodeWithText(text, useUnmergedTree = true)
            node.performScrollTo().assertIsDisplayed()
            assertReadable(node)
        }
        compose.onNodeWithText("Save and exit").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, saves) }
        capture("save-listing-progress-error-large-${if (darkTheme) "dark" else "light"}")
    }

    private fun assertReadable(node: SemanticsNodeInteraction) {
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayouts ->
            assertTrue(getLayouts(layouts))
        }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { layout ->
            val diagnostics = "text='${layout.layoutInput.text}', size=${layout.size}, " +
                "paragraphWidth=${layout.multiParagraph.width}, constraints=${layout.layoutInput.constraints}, " +
                "left=${layout.getLineLeft(0)}, right=${layout.getLineRight(0)}, bounds=${node.fetchSemanticsNode().boundsInRoot}"
            assertFalse("Text must not be clipped vertically: $diagnostics", layout.didOverflowHeight)
            repeat(layout.lineCount) { line ->
                assertFalse("Text must not be truncated: $diagnostics", layout.isLineEllipsized(line))
                // Centered text can retain a paragraph wider than its intrinsic-sized node.
                // Check the drawn line's width rather than its aligned paragraph offset.
                assertTrue("Text must fit its visible width: $diagnostics",
                    layout.getLineLeft(line) >= -1f &&
                        layout.getLineRight(line) <= layout.multiParagraph.width + 1f &&
                        layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width + 1f)
            }
        }
    }

    private fun capture(name: String) {
        val directory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "ui-review"
        ).apply { mkdirs() }
        compose.onNode(isDialog()).captureToImage().asAndroidBitmap().let { bitmap ->
            File(directory, "$name.png").outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }
}
