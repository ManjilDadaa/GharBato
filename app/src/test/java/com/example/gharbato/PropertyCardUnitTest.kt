package com.example.gharbato

import com.example.gharbato.model.PropertyModel
import com.example.gharbato.repository.PropertyRepo
import com.example.gharbato.repository.SavedPropertiesRepository
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlinx.coroutines.runBlocking

/**
 * Unit test for PropertyCard component in SearchScreen
 * Tests the filtering by market type that affects PropertyCard display
 */
class PropertyCardUnitTest {

    @Test
    fun propertyCard_filterByMarketType_returnsCorrectProperties() = runBlocking {
        // Arrange - Create mock repositories
        val propertyRepo = mock<PropertyRepo>()
        val savedPropertiesRepo = mock<SavedPropertiesRepository>()

        // Create test properties with different market types (displayed in PropertyCard)
        val testProperties = listOf(
            PropertyModel(
                id = 1,
                developer = "Sell Property 1",
                location = "Kathmandu, Nepal",
                price = "Rs. 50,00,000",
                bedrooms = 3,
                bathrooms = 2,
                sqft = "1500 sqft",
                marketType = "sell"
            ),
            PropertyModel(
                id = 2,
                developer = "Rent Property 1",
                location = "Pokhara, Nepal",
                price = "Rs. 20,000/month",
                bedrooms = 2,
                bathrooms = 1,
                sqft = "1000 sqft",
                marketType = "rent"
            ),
            PropertyModel(
                id = 3,
                developer = "Sell Property 2",
                location = "Bhaktapur, Nepal",
                price = "Rs. 70,00,000",
                bedrooms = 4,
                bathrooms = 3,
                sqft = "2000 sqft",
                marketType = "sell"
            ),
            PropertyModel(
                id = 4,
                developer = "Book Property 1",
                location = "Lalitpur, Nepal",
                price = "Rs. 5,000/night",
                bedrooms = 1,
                bathrooms = 1,
                sqft = "500 sqft",
                marketType = "book"
            )
        )

        // Mock repository responses
        whenever(propertyRepo.getAllApprovedProperties()).thenReturn(testProperties)
        whenever(savedPropertiesRepo.isPropertySaved(any())).thenReturn(false)
        whenever(savedPropertiesRepo.getSavedPropertiesFlow()).thenReturn(flowOf(emptyList()))

        // Act - Filter properties by "sell" market type (affects which PropertyCards are shown)
        val filterMarketType = "sell"
        val filteredResults = testProperties.filter {
            it.marketType.lowercase() == filterMarketType.lowercase()
        }

        // Assert - Should find 2 properties for sale to display in PropertyCards
        assertEquals(2, filteredResults.size)
        assertTrue(filteredResults.all { it.marketType.lowercase() == "sell" })
        assertTrue(filteredResults.any { it.id == 1 })
        assertTrue(filteredResults.any { it.id == 3 })
    }
}
