package com.example.gharbato

import com.example.gharbato.model.PropertyModel
import com.example.gharbato.model.PropertyStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Test for Pending Properties Filtering Logic
 *
 * This test verifies the business logic for filtering properties
 * to show only those with PENDING status that belong to the current user.
 *
 * Tests the filtering logic that would be used when navigating from
 * MyActivitiesActivity to PendingPropertiesActivity.
 */
class PendingPropertiesFilteringUnitTest {

    private lateinit var sampleProperties: List<PropertyModel>
    private val currentUserId = "testUser123"
    private val otherUserId = "otherUser456"

    @Before
    fun setup() {
        // Create sample properties with different statuses and owners
        sampleProperties = listOf(
            // User's pending properties
            PropertyModel(
                id = 1,
                ownerId = currentUserId,
                title = "Pending Property 1",
                description = "A beautiful house in Kathmandu",
                price = "5000000",
                location = "Kathmandu",
                propertyType = "House",
                bedrooms = 3,
                bathrooms = 2,
                sqft = "2000",
                status = PropertyStatus.PENDING,
                images = mapOf("cover" to listOf("https://example.com/image1.jpg")),
                createdAt = System.currentTimeMillis() - 1000000,
                latitude = 27.7172,
                longitude = 85.3240,
                ownerName = "Test User",
                ownerEmail = "test@example.com",
                developer = "ABC Developers",
                marketType = "Rent"
            ),
            PropertyModel(
                id = 2,
                ownerId = currentUserId,
                title = "Pending Property 2",
                description = "Modern apartment in Lalitpur",
                price = "3000000",
                location = "Lalitpur",
                propertyType = "Apartment",
                bedrooms = 2,
                bathrooms = 1,
                sqft = "1500",
                status = PropertyStatus.PENDING,
                images = mapOf("cover" to listOf("https://example.com/image2.jpg")),
                createdAt = System.currentTimeMillis() - 2000000,
                latitude = 27.6710,
                longitude = 85.3240,
                ownerName = "Test User",
                ownerEmail = "test@example.com",
                developer = "XYZ Builders",
                marketType = "Rent"
            ),
            // User's approved property (should be filtered out)
            PropertyModel(
                id = 3,
                ownerId = currentUserId,
                title = "Approved Property",
                description = "Prime land in Bhaktapur",
                price = "7000000",
                location = "Bhaktapur",
                propertyType = "Land",
                bedrooms = 0,
                bathrooms = 0,
                sqft = "3000",
                status = PropertyStatus.APPROVED,
                images = mapOf("cover" to listOf("https://example.com/image3.jpg")),
                createdAt = System.currentTimeMillis() - 3000000,
                latitude = 27.6710,
                longitude = 85.4298,
                ownerName = "Test User",
                ownerEmail = "test@example.com",
                developer = "Land Corp",
                marketType = "Sale"
            ),
            // User's rejected property (should be filtered out)
            PropertyModel(
                id = 4,
                ownerId = currentUserId,
                title = "Rejected Property",
                description = "Commercial space in Pokhara",
                price = "4000000",
                location = "Pokhara",
                propertyType = "Commercial",
                bedrooms = 0,
                bathrooms = 1,
                sqft = "1200",
                status = PropertyStatus.REJECTED,
                images = mapOf("cover" to listOf("https://example.com/image4.jpg")),
                createdAt = System.currentTimeMillis() - 4000000,
                latitude = 28.2096,
                longitude = 83.9856,
                ownerName = "Test User",
                ownerEmail = "test@example.com",
                developer = "Commercial Plus",
                marketType = "Rent"
            ),
            // Other user's pending property (should be filtered out)
            PropertyModel(
                id = 5,
                ownerId = otherUserId,
                title = "Other User's Pending Property",
                description = "House in Chitwan",
                price = "6000000",
                location = "Chitwan",
                propertyType = "House",
                bedrooms = 4,
                bathrooms = 3,
                sqft = "2500",
                status = PropertyStatus.PENDING,
                images = mapOf("cover" to listOf("https://example.com/image5.jpg")),
                createdAt = System.currentTimeMillis() - 5000000,
                latitude = 27.5291,
                longitude = 84.3542,
                ownerName = "Other User",
                ownerEmail = "other@example.com",
                developer = "Other Builders",
                marketType = "Sale"
            ),
            // Other user's approved property (should be filtered out)
            PropertyModel(
                id = 6,
                ownerId = otherUserId,
                title = "Other User's Approved Property",
                description = "Apartment in Biratnagar",
                price = "8000000",
                location = "Biratnagar",
                propertyType = "Apartment",
                bedrooms = 3,
                bathrooms = 2,
                sqft = "1800",
                status = PropertyStatus.APPROVED,
                images = mapOf("cover" to listOf("https://example.com/image6.jpg")),
                createdAt = System.currentTimeMillis() - 6000000,
                latitude = 26.4525,
                longitude = 87.2718,
                ownerName = "Other User",
                ownerEmail = "other@example.com",
                developer = "Eastern Developers",
                marketType = "Rent"
            )
        )
    }

    /**
     * TEST: Filter Properties to Show Only Current User's Pending Listings
     *
     * Verifies that when navigating to PendingPropertiesActivity,
     * only the properties that:
     * 1. Belong to the current user (ownerId matches)
     * 2. Have PENDING status
     * are displayed.
     *
     * This simulates the filtering logic used in PendingPropertiesActivity
     * when loading properties from Firebase.
     */
    @Test
    fun filterProperties_whenClickedPending_shouldShowOnlyUserPendingListings() {
        // ACT - Apply the same filtering logic used in PendingPropertiesActivity
        val pendingProperties = sampleProperties.filter { property ->
            property.ownerId == currentUserId && property.status == PropertyStatus.PENDING
        }.sortedByDescending { it.createdAt }

        // ASSERT - Verify correct properties are filtered

        // 1. Verify correct count - should be 2 pending properties for this user
        assertEquals(
            "Should have exactly 2 pending properties for current user",
            2,
            pendingProperties.size
        )

        // 2. Verify all returned properties belong to current user
        assertTrue(
            "All properties should belong to current user",
            pendingProperties.all { it.ownerId == currentUserId }
        )

        // 3. Verify all returned properties have PENDING status
        assertTrue(
            "All properties should have PENDING status",
            pendingProperties.all { it.status == PropertyStatus.PENDING }
        )

        // 4. Verify specific properties are included
        val propertyIds = pendingProperties.map { it.id }
        assertTrue(
            "Should include property with id 1",
            propertyIds.contains(1)
        )
        assertTrue(
            "Should include property with id 2",
            propertyIds.contains(2)
        )

        // 5. Verify approved property is NOT included
        assertFalse(
            "Should NOT include approved property (id 3)",
            propertyIds.contains(3)
        )

        // 6. Verify rejected property is NOT included
        assertFalse(
            "Should NOT include rejected property (id 4)",
            propertyIds.contains(4)
        )

        // 7. Verify other user's properties are NOT included
        assertFalse(
            "Should NOT include other user's pending property (id 5)",
            propertyIds.contains(5)
        )
        assertFalse(
            "Should NOT include other user's approved property (id 6)",
            propertyIds.contains(6)
        )

        // 8. Verify properties are sorted by creation date (newest first)
        assertEquals(
            "First property should be the most recently created",
            1,
            pendingProperties[0].id
        )
        assertEquals(
            "Second property should be the second most recently created",
            2,
            pendingProperties[1].id
        )

        // 9. Verify property details are correct for first pending property
        val firstProperty = pendingProperties[0]
        assertEquals("Pending Property 1", firstProperty.title)
        assertEquals("5000000", firstProperty.price)
        assertEquals("Kathmandu", firstProperty.location)
        assertEquals("House", firstProperty.propertyType)

        // 10. Verify property details are correct for second pending property
        val secondProperty = pendingProperties[1]
        assertEquals("Pending Property 2", secondProperty.title)
        assertEquals("3000000", secondProperty.price)
        assertEquals("Lalitpur", secondProperty.location)
        assertEquals("Apartment", secondProperty.propertyType)
    }

    /**
     * Additional Test: Empty Pending Properties List
     *
     * Verifies behavior when user has no pending properties
     */
    @Test
    fun filterProperties_whenNoPendingProperties_shouldReturnEmptyList() {
        // ARRANGE - Create properties with no pending status for current user
        val propertiesWithoutPending = listOf(
            PropertyModel(
                id = 1,
                ownerId = currentUserId,
                title = "Approved Property",
                description = "Approved house",
                price = "5000000",
                location = "Kathmandu",
                propertyType = "House",
                bedrooms = 3,
                bathrooms = 2,
                sqft = "2000",
                status = PropertyStatus.APPROVED,
                images = mapOf("cover" to listOf("https://example.com/image1.jpg")),
                createdAt = System.currentTimeMillis(),
                latitude = 27.7172,
                longitude = 85.3240,
                ownerName = "Test User",
                ownerEmail = "test@example.com",
                developer = "ABC Developers",
                marketType = "Rent"
            ),
            PropertyModel(
                id = 2,
                ownerId = currentUserId,
                title = "Rejected Property",
                description = "Rejected apartment",
                price = "3000000",
                location = "Lalitpur",
                propertyType = "Apartment",
                bedrooms = 2,
                bathrooms = 1,
                sqft = "1500",
                status = PropertyStatus.REJECTED,
                images = mapOf("cover" to listOf("https://example.com/image2.jpg")),
                createdAt = System.currentTimeMillis(),
                latitude = 27.6710,
                longitude = 85.3240,
                ownerName = "Test User",
                ownerEmail = "test@example.com",
                developer = "XYZ Builders",
                marketType = "Rent"
            )
        )

        // ACT - Apply filtering
        val pendingProperties = propertiesWithoutPending.filter { property ->
            property.ownerId == currentUserId && property.status == PropertyStatus.PENDING
        }

        // ASSERT - Should return empty list
        assertTrue(
            "Should return empty list when no pending properties exist",
            pendingProperties.isEmpty()
        )
        assertEquals(
            "List size should be 0",
            0,
            pendingProperties.size
        )
    }

    /**
     * Additional Test: All Properties are Pending
     *
     * Verifies correct behavior when all user's properties are pending
     */
    @Test
    fun filterProperties_whenAllPropertiesPending_shouldReturnAllUserProperties() {
        // ARRANGE - Create properties where all of user's properties are pending
        val allPendingProperties = listOf(
            PropertyModel(
                id = 1,
                ownerId = currentUserId,
                title = "Pending Property 1",
                description = "First pending house",
                price = "5000000",
                location = "Kathmandu",
                propertyType = "House",
                bedrooms = 3,
                bathrooms = 2,
                sqft = "2000",
                status = PropertyStatus.PENDING,
                images = mapOf("cover" to listOf("https://example.com/image1.jpg")),
                createdAt = System.currentTimeMillis(),
                latitude = 27.7172,
                longitude = 85.3240,
                ownerName = "Test User",
                ownerEmail = "test@example.com",
                developer = "ABC Developers",
                marketType = "Rent"
            ),
            PropertyModel(
                id = 2,
                ownerId = currentUserId,
                title = "Pending Property 2",
                description = "Second pending apartment",
                price = "3000000",
                location = "Lalitpur",
                propertyType = "Apartment",
                bedrooms = 2,
                bathrooms = 1,
                sqft = "1500",
                status = PropertyStatus.PENDING,
                images = mapOf("cover" to listOf("https://example.com/image2.jpg")),
                createdAt = System.currentTimeMillis(),
                latitude = 27.6710,
                longitude = 85.3240,
                ownerName = "Test User",
                ownerEmail = "test@example.com",
                developer = "XYZ Builders",
                marketType = "Rent"
            ),
            PropertyModel(
                id = 3,
                ownerId = currentUserId,
                title = "Pending Property 3",
                description = "Third pending land",
                price = "4000000",
                location = "Bhaktapur",
                propertyType = "Land",
                bedrooms = 0,
                bathrooms = 0,
                sqft = "2500",
                status = PropertyStatus.PENDING,
                images = mapOf("cover" to listOf("https://example.com/image3.jpg")),
                createdAt = System.currentTimeMillis(),
                latitude = 27.6710,
                longitude = 85.4298,
                ownerName = "Test User",
                ownerEmail = "test@example.com",
                developer = "Land Corp",
                marketType = "Sale"
            )
        )

        // ACT - Apply filtering
        val pendingProperties = allPendingProperties.filter { property ->
            property.ownerId == currentUserId && property.status == PropertyStatus.PENDING
        }

        // ASSERT - Should return all 3 properties
        assertEquals(
            "Should return all 3 properties when all are pending",
            3,
            pendingProperties.size
        )

        assertTrue(
            "All properties should have PENDING status",
            pendingProperties.all { it.status == PropertyStatus.PENDING }
        )
    }
}