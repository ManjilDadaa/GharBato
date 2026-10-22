package com.example.gharbato

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.view.PurposeContentScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PurposeSelectionInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun darkSelectionsStayVisibleAndUsableWithLargeText() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                GharBatoTheme(darkTheme = true) {
                    var purpose by remember { mutableStateOf("Sell") }
                    var propertyType by remember { mutableStateOf("Apartment") }
                    Column(
                        Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        PurposeContentScreen(purpose, { purpose = it }, propertyType, { propertyType = it })
                    }
                }
            }
        }

        compose.onNodeWithText("Sell Property").assertIsSelected()
        compose.onNodeWithText("Rent Out Property").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Sell Property").assertIsNotSelected()
        assertSelectedCardIsBrighter("Rent Out Property", "Sell Property")
        compose.onNodeWithText("Find tenants for long-term rental").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("Apartment").performScrollTo().assertIsSelected()
        assertSelectedCardIsBrighter("Apartment", "House")
        compose.onNodeWithText("House").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Apartment").assertIsNotSelected()
        compose.onNodeWithText("Studio").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("House").assertIsNotSelected()
    }

    private fun assertSelectedCardIsBrighter(selectedLabel: String, unselectedLabel: String) {
        val selectedImage = compose.onNodeWithText(selectedLabel).performScrollTo().captureToImage()
        val selectedFill = selectedImage.toPixelMap()[selectedImage.width / 2, selectedImage.height / 10]
        val unselectedImage = compose.onNodeWithText(unselectedLabel).performScrollTo().captureToImage()
        val unselectedFill = unselectedImage.toPixelMap()[unselectedImage.width / 2, unselectedImage.height / 10]
        assertTrue("Selected cards should stand out from unselected cards in dark mode", selectedFill.luminance() > unselectedFill.luminance() + 0.05f)
        assertTrue("Selected cards should retain the purple brand color", selectedFill.red > selectedFill.green && selectedFill.blue > selectedFill.green)
    }
}
