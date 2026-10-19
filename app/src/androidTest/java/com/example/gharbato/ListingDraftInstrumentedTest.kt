package com.example.gharbato

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.utils.ListingDraftStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ListingDraftInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val userId = "ui-test-${UUID.randomUUID()}"
    private val stores = mutableListOf<ListingDraftStore>()
    private fun store(user: String = userId, property: String? = null) =
        ListingDraftStore(context, user, property).also { stores.add(it) }

    @After fun cleanUp() { stores.forEach { it.clear() } }

    @Test fun draftRestoresDetailsTermsAmenitiesAndStep() {
        val original = PropertyListingState(
            selectedPurpose = "Rent", selectedPropertyType = "Apartment", title = "Sunny apartment in Patan",
            price = "25500", area = "1200", location = "Patan, Lalitpur", bedrooms = "2", bathrooms = "1",
            latitude = 27.67, longitude = 85.32, hasSelectedLocation = true,
            furnishing = "Unfurnished", parking = false, petsAllowed = true, description = "A bright home near the square.",
            minimumLease = "6 months", securityDeposit = "1 month rent", amenities = listOf("Balcony", "Water Supply 24/7")
        )
        assertTrue(store().save(original, 4))
        val restored = store().load()!!
        assertEquals(original, restored.state)
        assertEquals(4, restored.step)
    }

    @Test fun photosSurviveTheOriginalPickerFileDisappearing() {
        val source = File.createTempFile("draft-photo", ".jpg", context.cacheDir)
        val bytes = "draft photo fixture".toByteArray()
        source.writeBytes(bytes)
        val state = PropertyListingState()
        state.imageCategories.first().images.add(Uri.fromFile(source).toString())
        try {
            assertTrue(store().save(state, 3))
            source.delete()
            val photo = store().load()!!.state.imageCategories.first().images.single()
            assertArrayEquals(bytes, context.contentResolver.openInputStream(Uri.parse(photo))!!.use { it.readBytes() })
        } finally {
            source.delete()
        }
    }

    @Test fun draftsAreIsolatedByAccountAndEditedProperty() {
        val own = store()
        assertTrue(own.save(PropertyListingState(title = "My unfinished listing"), 2))
        assertNull(store(user = "$userId-other").load())
        assertNull(store(property = "existing-property").load())
        own.clear()
        assertNull(own.load())
    }

    @Test fun failedPhotoCopyPreservesThePreviousDraft() {
        val drafts = store()
        assertTrue(drafts.save(PropertyListingState(title = "Previous draft"), 2))
        val unavailable = PropertyListingState(title = "Unsaved changes")
        unavailable.imageCategories.first().images.add("content://com.example.gharbato.missing/photo")
        assertFalse(drafts.save(unavailable, 3))
        assertEquals("Previous draft", drafts.load()!!.state.title)
    }
}
