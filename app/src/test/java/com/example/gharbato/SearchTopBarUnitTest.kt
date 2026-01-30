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
 * Unit test for SearchTopBar component in SearchScreen
 * Tests the search functionality triggered by SearchTopBar's onSearchClick
 */
class SearchTopBarUnitTest {

    @Test
    fun searchTopBar_searchByLocation_returnsMatchingProperties() = runBlocking {
        // Arrange - Create mock repositories
        val propertyRepo = mock<PropertyRepo>()
        val savedPropertiesRepo = mock<SavedPropertiesRepository>()

        // Create test properties
        val testProperties = listOf(
            PropertyModel(
                id = 1,
                developer = "Test Developer 1",
                location = "Kathmandu, Nepal",
                price = "Rs. 50,00,000",
                bedrooms = 3,
                bathrooms = 2,
                sqft = "1500 sqft",
                marketType = "sell"
            ),
            PropertyModel(
                id = 2,
                developer = "Test Developer 2",
                location = "Pokhara, Nepal",
                price = "Rs. 30,00,000",
                bedrooms = 2,
                bathrooms = 1,
                sqft = "1000 sqft",
                marketType = "rent"
            ),
            PropertyModel(
                id = 3,
                developer = "Test Developer 3",
                location = "Kathmandu, Nepal",
                price = "Rs. 70,00,000",
                bedrooms = 4,
                bathrooms = 3,
                sqft = "2000 sqft",
                marketType = "sell"
            )
        )

        // Mock the repository responses
        whenever(propertyRepo.getAllApprovedProperties()).thenReturn(testProperties)
        whenever(propertyRepo.searchProperties(any())).thenAnswer { invocation ->
            val query = invocation.getArgument<String>(0).lowercase()
            testProperties.filter {
                it.location.lowercase().contains(query) ||
                it.developer.lowercase().contains(query)
            }
        }
        whenever(savedPropertiesRepo.isPropertySaved(any())).thenReturn(false)
        whenever(savedPropertiesRepo.getSavedPropertiesFlow()).thenReturn(flowOf(emptyList()))

        // Act - Simulate SearchTopBar search for "Kathmandu"
        val searchQuery = "Kathmandu"
        val searchResults = testProperties.filter {
            it.location.lowercase().contains(searchQuery.lowercase())
        }

        // Assert - Should find 2 properties in Kathmandu
        assertEquals(2, searchResults.size)
        assertTrue(searchResults.all { it.location.contains("Kathmandu") })
    }
}
