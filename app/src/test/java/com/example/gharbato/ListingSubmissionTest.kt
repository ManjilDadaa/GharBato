package com.example.gharbato

import com.example.gharbato.utils.resolveListingImages
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.repository.PropertyRepo
import com.example.gharbato.viewmodel.ListingViewModel
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class ListingSubmissionTest {
    @Test fun replacementCoverStaysCoverWhenBedroomAlreadyExists() {
        val result = resolveListingImages(linkedMapOf(
            "cover" to listOf("content://new-cover"),
            "bedrooms" to listOf("https://existing-bedroom")
        ), listOf("https://uploaded-cover"))
        assertEquals(listOf("https://uploaded-cover"), result["cover"])
        assertEquals(listOf("https://existing-bedroom"), result["bedrooms"])
    }

    @Test fun interleavedNewAndExistingPhotosKeepTheirPositions() {
        val result = resolveListingImages(linkedMapOf(
            "cover" to listOf("https://cover"),
            "bedrooms" to listOf("content://one", "https://existing", "file://two"),
            "kitchen" to listOf("content://three")
        ), listOf("https://one", "https://two", "https://three"))
        assertEquals(listOf("https://one", "https://existing", "https://two"), result["bedrooms"])
        assertEquals(listOf("https://three"), result["kitchen"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun partialUploadCannotProduceAListing() {
        resolveListingImages(mapOf("cover" to listOf("content://one", "content://two")), listOf("https://one"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankUploadCannotProduceAListing() {
        resolveListingImages(mapOf("cover" to listOf("content://one")), listOf(""))
    }

    @Test fun existingPhotosNeedNoUpload() {
        val images = mapOf("cover" to listOf("https://cover"))
        assertEquals(images, resolveListingImages(images, emptyList()))
    }

    @Test fun finalSellAndStayStepValidatesAmenitiesInsteadOfRentalFields() {
        val vm = ListingViewModel(mock(PropertyRepo::class.java))
        for (purpose in listOf("Sell", "Book")) {
            val state = PropertyListingState(selectedPurpose = purpose, minimumLease = "")
            assertFalse(vm.validateStep(4, state).isValid)
            assertTrue(vm.validateStep(4, state.copy(amenities = listOf("WiFi Internet"))).isValid)
        }
        assertFalse(vm.validateStep(4, PropertyListingState(selectedPurpose = "Rent", minimumLease = "")).isValid)
    }
}
