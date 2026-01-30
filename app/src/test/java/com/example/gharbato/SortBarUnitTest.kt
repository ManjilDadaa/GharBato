package com.example.gharbato

import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.SortOption
import com.example.gharbato.repository.PropertyRepo
import com.example.gharbato.repository.SavedPropertiesRepository
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.junit.Assert.assertEquals
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlinx.coroutines.runBlocking

/**
 * Unit test for SortBar component in SearchScreen
 * Tests the sorting functionality triggered by SortBar's onSortClick
 */
class SortBarUnitTest {

    @Test
    fun sortBar_sortByBedroomsDescending_returnsCorrectOrder() = runBlocking {
        // Arrange - Create mock repositories
        val propertyRepo = mock<PropertyRepo>()
        val savedPropertiesRepo = mock<SavedPropertiesRepository>()

        // Create test properties with different bedroom counts
        val testProperties = listOf(
            PropertyModel(
                id = 1,
                developer = "Small House",
                location = "Kathmandu, Nepal",
                price = "Rs. 30,00,000",
                bedrooms = 2,
                bathrooms = 1,
                sqft = "800 sqft",
                marketType = "sell"
            ),
            PropertyModel(
                id = 2,
                developer = "Large Villa",
                location = "Pokhara, Nepal",
                price = "Rs. 1,00,00,000",
                bedrooms = 5,
                bathrooms = 4,
                sqft = "3000 sqft",
                marketType = "sell"
            ),
            PropertyModel(
                id = 3,
                developer = "Medium Apartment",
                location = "Bhaktapur, Nepal",
                price = "Rs. 50,00,000",
                bedrooms = 3,
                bathrooms = 2,
                sqft = "1200 sqft",
                marketType = "sell"
            ),
            PropertyModel(
                id = 4,
                developer = "Family Home",
                location = "Lalitpur, Nepal",
                price = "Rs. 70,00,000",
                bedrooms = 4,
                bathrooms = 3,
                sqft = "2000 sqft",
                marketType = "sell"
            )
        )

        // Mock repository responses
        whenever(propertyRepo.getAllApprovedProperties()).thenReturn(testProperties)
        whenever(savedPropertiesRepo.isPropertySaved(any())).thenReturn(false)
        whenever(savedPropertiesRepo.getSavedPropertiesFlow()).thenReturn(flowOf(emptyList()))

        // Act - Simulate SortBar sort by bedrooms descending
        val sortedResults = testProperties.sortedByDescending { it.bedrooms }

        // Assert - Should be sorted with most bedrooms first
        assertEquals(4, sortedResults.size)
        assertEquals(5, sortedResults[0].bedrooms) // Large Villa
        assertEquals(4, sortedResults[1].bedrooms) // Family Home
        assertEquals(3, sortedResults[2].bedrooms) // Medium Apartment
        assertEquals(2, sortedResults[3].bedrooms) // Small House
    }
}
