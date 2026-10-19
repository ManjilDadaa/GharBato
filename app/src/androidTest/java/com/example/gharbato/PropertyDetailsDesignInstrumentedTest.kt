package com.example.gharbato

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gharbato.model.PropertyModel
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.view.PropertyDetailsContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Pure Compose coverage: no activity navigation, Firebase reads, remote photos, or Maps. */
@RunWith(AndroidJUnit4::class)
class PropertyDetailsDesignInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private val property = PropertyModel(
        id = 42,
        title = "Bright two-bedroom apartment",
        developer = "Legacy building name",
        price = "25500",
        marketType = "Rent",
        location = "Bhaisepati, Lalitpur",
        bedrooms = 2,
        bathrooms = 1,
        sqft = "1,100 sq.ft",
        ownerId = "owner",
        ownerName = "Sujata Shrestha",
        description = "A bright apartment with a private balcony and an open living area.",
        amenities = listOf("Parking", "Balcony"),
        latitude = 0.0,
        longitude = 0.0
    )

    @Test fun lightDetailsShowTheListingAndToolbarActionsWork() {
        var backs = 0
        var shares = 0
        var messages = 0
        var calls = 0
        compose.setContent {
            var saved by remember { mutableStateOf(false) }
            GharBatoTheme(darkTheme = false) {
                PropertyDetailsContent(
                    property = property.copy(isFavorite = saved),
                    onBack = { backs++ },
                    onFavoriteClick = { saved = !saved },
                    onShareClick = { shares++ },
                    onMessageClick = { messages++ },
                    onCallClick = { calls++ }
                )
            }
        }

        compose.onNodeWithText(property.title).assertIsDisplayed()
        compose.onNodeWithText("Rs 25,500/month").assertIsDisplayed()
        compose.onNodeWithText("No photos available").assertIsDisplayed()
        assertUnsupportedTrustBadgesAbsent()
        capture("property-details-light")

        compose.onNodeWithContentDescription("Save property").performClick()
        compose.onNodeWithContentDescription("Remove from saved")
            .assertIsToggleable().assertIsOn()
        compose.onNodeWithContentDescription("Share property").performClick()
        compose.onNodeWithText("Message owner").performClick()
        compose.onNodeWithText("Call").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle {
            assertEquals(1, backs)
            assertEquals(1, shares)
            assertEquals(1, messages)
            assertEquals(1, calls)
        }
    }

    @Test fun darkDetailsKeepTheOwnerAndRealContentWithoutInventedTrustClaims() {
        var offers = 0
        var reports = 0
        compose.setContent {
            GharBatoTheme(darkTheme = true) {
                PropertyDetailsContent(
                    property = property,
                    onOfferClick = { offers++ },
                    onReportClick = { reports++ }
                )
            }
        }
        compose.onNodeWithTag("property_detail_list")
            .performScrollToNode(hasText(property.description!!))
        compose.onNodeWithText(property.description!!).assertIsDisplayed()
        compose.onNodeWithTag("property_detail_list")
            .performScrollToNode(hasText(property.ownerName))
        compose.onNodeWithText(property.ownerName).assertIsDisplayed()
        assertUnsupportedTrustBadgesAbsent()
        compose.onNodeWithText("Message owner").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Call").assertIsDisplayed().assertHasClickAction()
        capture("property-details-dark-owner")
        compose.onNodeWithTag("property_detail_list")
            .performScrollToNode(hasText("Make an offer"))
        compose.onNodeWithText("Make an offer").performClick()
        compose.onNodeWithTag("property_detail_list")
            .performScrollToNode(hasText("Report listing"))
        compose.onNodeWithText("Report listing").performClick()
        compose.runOnIdle {
            assertEquals(1, offers)
            assertEquals(1, reports)
        }
        compose.onNodeWithTag("property_detail_list").performScrollToIndex(0)
        capture("property-details-dark")
    }

    @Test fun emptyOptionalSectionsAreOmittedAndKeyFactsAreNotRepeatedInDetails() {
        compose.setContent {
            GharBatoTheme(darkTheme = false) {
                PropertyDetailsContent(
                    property = property.copy(
                        marketType = "Sell",
                        price = "18500000",
                        description = "",
                        amenities = emptyList(),
                        utilitiesIncluded = "",
                        commission = "",
                        advancePayment = "",
                        securityDeposit = "",
                        minimumLease = "",
                        availableFrom = ""
                    )
                )
            }
        }
        compose.onNodeWithText("Rs 18,500,000").assertIsDisplayed()
        compose.onNodeWithTag("property_detail_list")
            .performScrollToNode(hasText("Property details"))
        // Bedroom, bathroom, and area belong to the compact summary, not a second table.
        listOf("Bedrooms", "Bathroom", "Bathrooms", "sq.ft").forEach { label ->
            assertTrue(
                "The '$label' fact must not be repeated",
                compose.onAllNodesWithText(label).fetchSemanticsNodes().size <= 1
            )
        }
        compose.onNodeWithTag("property_detail_list")
            .performScrollToNode(hasText("Listed by"))
        compose.onNodeWithText(property.ownerName).assertIsDisplayed()
        listOf("About this property", "Amenities", "Rental terms", "Similar homes")
            .forEach(::assertSectionAbsentThroughoutList)
    }

    @Test fun largeTextKeepsToolbarAndContactActionsAvailableWhileReading() {
        var backs = 0
        var messages = 0
        var calls = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f)
            ) {
                GharBatoTheme(darkTheme = true) {
                    PropertyDetailsContent(
                        property = property.copy(
                            title = "Spacious family apartment with a private balcony in Bhaisepati",
                            ownerName = "Sujata Shrestha, property owner in Lalitpur"
                        ),
                        onBack = { backs++ },
                        onMessageClick = { messages++ },
                        onCallClick = { calls++ }
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Back").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithContentDescription("Save property").assertIsDisplayed()
        compose.onNodeWithContentDescription("Share property").assertIsDisplayed()
        compose.onNodeWithText("Message owner").assertIsDisplayed().performClick()
        compose.onNodeWithText("Call").assertIsDisplayed().performClick()
        capture("property-details-large-font")

        compose.onNodeWithTag("property_detail_list")
            .performScrollToNode(hasText("Sujata Shrestha, property owner in Lalitpur"))
        compose.onNodeWithText("Message owner").assertIsDisplayed().performClick()
        compose.onNodeWithText("Call").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
        capture("property-details-large-font-owner")
        compose.runOnIdle {
            assertEquals(1, backs)
            assertEquals(2, messages)
            assertEquals(2, calls)
        }
    }

    @Test fun missingOwnerCannotTriggerUnusableContactActions() {
        var contacts = 0
        compose.setContent {
            GharBatoTheme(darkTheme = false) {
                PropertyDetailsContent(
                    property = property.copy(ownerId = "", ownerName = ""),
                    onMessageClick = { contacts++ },
                    onCallClick = { contacts++ }
                )
            }
        }
        compose.onNodeWithText("Call").assertIsNotEnabled()
        compose.onNodeWithText("Message owner").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, contacts) }
    }

    @Test fun galleryCanSwipeBetweenLocalImagesAndKeepsActionsVisible() {
        val cover = localGalleryImage("property-detail-cover.png", android.graphics.Color.LTGRAY)
        val interior = localGalleryImage("property-detail-interior.png", android.graphics.Color.DKGRAY)
        try {
            compose.setContent {
                GharBatoTheme(darkTheme = false) {
                    PropertyDetailsContent(
                        property = property.copy(
                            images = linkedMapOf(
                                "interior" to listOf(android.net.Uri.fromFile(interior).toString()),
                                "cover" to listOf(android.net.Uri.fromFile(cover).toString())
                            )
                        )
                    )
                }
            }
            compose.onNodeWithText("No photos available").assertDoesNotExist()
            compose.onNodeWithText("1 / 2").assertIsDisplayed()
            compose.onNodeWithTag("property_detail_gallery").performTouchInput { swipeLeft() }
            compose.onNodeWithText("2 / 2").assertIsDisplayed()
            compose.onNodeWithContentDescription("View all photos").performClick()
            compose.onNodeWithContentDescription("Close gallery").assertIsDisplayed().performClick()
            compose.onNodeWithText("2 / 2").assertIsDisplayed()
            compose.onNodeWithText("Message owner").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithContentDescription("Back").assertIsDisplayed()
        } finally {
            cover.delete()
            interior.delete()
        }
    }

    private fun assertUnsupportedTrustBadgesAbsent() {
        listOf("Verified", "Verified Owner", "Featured", "Online")
            .forEach { compose.onNodeWithText(it, ignoreCase = true).assertDoesNotExist() }
    }

    private fun assertSectionAbsentThroughoutList(title: String) {
        var found = true
        try {
            compose.onNodeWithTag("property_detail_list").performScrollToNode(hasText(title))
        } catch (_: AssertionError) {
            found = false
        }
        assertFalse("An empty '$title' section must not be rendered", found)
    }

    private fun capture(name: String) {
        val directory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "ui-review"
        ).apply { mkdirs() }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(directory, "$name.png").outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }

    private fun localGalleryImage(name: String, color: Int): File {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, name)
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }
}
