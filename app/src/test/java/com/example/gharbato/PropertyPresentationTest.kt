package com.example.gharbato

import com.example.gharbato.utils.formatPropertyPrice
import com.example.gharbato.utils.hasPropertyCoordinates
import com.example.gharbato.utils.isWithinPriceRange
import org.junit.Assert.*
import org.junit.Test

class PropertyPresentationTest {
    @Test fun rawPricesIncludeCurrencyAndTheCorrectPeriod() {
        assertEquals("Rs 25,000/month", formatPropertyPrice("25000", "Rent"))
        assertEquals("Rs 3,500/night", formatPropertyPrice("3500", "Book"))
        assertEquals("Rs 5,000,000", formatPropertyPrice("5000000", "Sell"))
    }

    @Test fun legacyFormattedPricesDoNotDuplicateCurrencyOrPeriod() {
        assertEquals("Rs 25,000/month", formatPropertyPrice("Rs. 25,000/month", "Rent"))
        assertEquals("Rs 5,000,000", formatPropertyPrice("NPR 50,00,000", "Sell"))
        assertEquals("Rs 25,500.5/month", formatPropertyPrice("25500.50", "rent"))
    }

    @Test fun unstructuredPricesRemainReadable() {
        assertEquals("Negotiable", formatPropertyPrice("Negotiable", "Rent"))
        assertEquals("Price on request", formatPropertyPrice("", "Sell"))
    }

    @Test fun priceFiltersUseWholeRupeesAndIncludeTheirBounds() {
        assertTrue(isWithinPriceRange("Rs. 25,500/month", 25500, 25500))
        assertFalse(isWithinPriceRange("25500/month", 25501, 0))
        assertFalse(isWithinPriceRange("25500/month", 0, 25499))
    }

    @Test fun priceFiltersHandleDecimalsAndLargeSalePrices() {
        assertFalse(isWithinPriceRange("Rs 25,000.50/month", 0, 25000))
        assertTrue(isWithinPriceRange("Rs 50,00,000", 4999999, 5000000))
        assertTrue(isWithinPriceRange("3000000000", 1000000, 0))
    }

    @Test fun unknownPricesOnlyMatchWhenNoBudgetIsSpecified() {
        assertFalse(isWithinPriceRange("Price on request", 10000, 0))
        assertTrue(isWithinPriceRange("Price on request", 0, 0))
    }

    @Test fun missingAndLegacyDefaultCoordinatesDoNotBecomePropertyPins() {
        assertFalse(hasPropertyCoordinates(0.0, 0.0))
        assertFalse(hasPropertyCoordinates(-0.0, -0.0))
        assertFalse(hasPropertyCoordinates(27.7172, 85.3240))
    }

    @Test fun realLocationsRemainValidIncludingTheEquatorAndPrimeMeridian() {
        assertTrue(hasPropertyCoordinates(27.6727, 85.3105)) // Lalitpur
        assertTrue(hasPropertyCoordinates(28.2096, 83.9856)) // Pokhara
        assertTrue(hasPropertyCoordinates(27.71721, 85.3240)) // Near the legacy default
        assertTrue(hasPropertyCoordinates(0.0, 85.3240))
        assertTrue(hasPropertyCoordinates(27.7172, 0.0))
    }

    @Test fun corruptOrOutOfRangeCoordinatesCannotInitializeAMap() {
        listOf(
            Double.NaN to 85.3,
            27.7 to Double.NaN,
            Double.POSITIVE_INFINITY to 85.3,
            27.7 to Double.NEGATIVE_INFINITY,
            90.0001 to 85.3,
            -90.0001 to 85.3,
            27.7 to 180.0001,
            27.7 to -180.0001
        ).forEach { (latitude, longitude) ->
            assertFalse(
                "Invalid coordinates ($latitude, $longitude) must not be shown",
                hasPropertyCoordinates(latitude, longitude)
            )
        }
    }

    @Test fun validCoordinateBoundsAreInclusive() {
        assertTrue(hasPropertyCoordinates(90.0, 180.0))
        assertTrue(hasPropertyCoordinates(-90.0, -180.0))
    }
}
