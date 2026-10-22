package com.example.gharbato

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.utils.ListingDraftStore
import com.example.gharbato.view.ListingBody
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ListingPageScrollingInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val stores = mutableListOf<ListingDraftStore>()

    @After fun removeTestDrafts() { stores.forEach { it.clear() } }

    @Test fun restoredDetailsScrollToDescriptionUnderThePinnedProgress() {
        openDraft(step = 2, heading = "Property Details")
        verifyParentScroll(
            destination = "Description",
            heading = "Property Details",
            expectedStep = "Step 2 of 5",
            captureName = "listing-details-parent-scroll"
        )
    }

    @Test fun restoredPhotosScrollToTheLastTipUnderThePinnedProgress() {
        openDraft(step = 3, heading = "Property Photos")
        verifyParentScroll(
            destination = "Horizontal orientation works best",
            heading = "Property Photos",
            expectedStep = "Step 3 of 5",
            captureName = "listing-photos-parent-scroll"
        )
    }

    private fun openDraft(step: Int, heading: String) {
        val propertyId = "listing-scroll-test-${UUID.randomUUID()}"
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val store = ListingDraftStore(context, userId, propertyId).also { stores.add(it) }
        assertTrue(store.save(PropertyListingState(
            selectedPurpose = "Rent", selectedPropertyType = "Apartment",
            title = "Scrolling test draft", price = "25000", area = "1200",
            location = "Patan, Lalitpur", bedrooms = "2", bathrooms = "1",
            description = "A local draft used to verify the listing form scrolls."
        ), step))
        compose.setContent {
            GharBatoTheme(darkTheme = false) {
                Box(Modifier.width(320.dp).height(560.dp)) {
                    ListingBody(propertyId = propertyId, isEdit = false)
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(heading).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(heading).assertIsDisplayed()
    }

    private fun verifyParentScroll(destination: String, heading: String, expectedStep: String, captureName: String) {
        val progress = compose.onNodeWithTag("listing_progress")
        val appBar = compose.onNodeWithTag("listing_app_bar")
        val form = compose.onNodeWithTag("listing_form_scroll")
        progress.assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expectedStep))
        val initialProgressTop = progress.fetchSemanticsNode().boundsInRoot.top

        form.performScrollToNode(hasText(destination))
        compose.onNodeWithText(destination).assertIsDisplayed()
        progress.assertIsDisplayed()
        appBar.assertIsDisplayed()
        assertEquals(initialProgressTop, progress.fetchSemanticsNode().boundsInRoot.top, 1f)
        assertTrue(
            "The pinned progress must stay below the app bar",
            progress.fetchSemanticsNode().boundsInRoot.top >= appBar.fetchSemanticsNode().boundsInRoot.bottom - 1f
        )
        compose.onNodeWithText("Next").assertIsDisplayed()
        capture(captureName)

        form.performScrollToNode(hasText(heading))
        compose.onNodeWithText(heading).assertIsDisplayed()
        progress.assertIsDisplayed()
        assertEquals(initialProgressTop, progress.fetchSemanticsNode().boundsInRoot.top, 1f)
    }

    private fun capture(name: String) {
        val folder = File(context.getExternalFilesDir(null), "ui-review").apply { mkdirs() }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
