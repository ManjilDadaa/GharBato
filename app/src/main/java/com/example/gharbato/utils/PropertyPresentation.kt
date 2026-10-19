package com.example.gharbato.utils

import java.text.NumberFormat
import java.util.Locale

/** A stored default or missing coordinate must never be presented as the property's real address. */
fun hasPropertyCoordinates(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0) &&
        !(latitude == 27.7172 && longitude == 85.3240)

fun formatNprAmount(amount: Number): String =
    "Rs ${NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }.format(amount)}"

fun isWithinPriceRange(price: String, minimumNpr: Int, maximumNpr: Int): Boolean {
    if (minimumNpr == 0 && maximumNpr == 0) return true
    val amount = Regex("[0-9][0-9,]*(?:\\.[0-9]+)?").find(price.substringBefore('/'))
        ?.value?.replace(",", "")?.toBigDecimalOrNull() ?: return false
    return amount >= minimumNpr.toBigDecimal() &&
        (maximumNpr == 0 || amount <= maximumNpr.toBigDecimal())
}

/** Accepts both older currency-prefixed listings and the raw amounts saved by the listing form. */
fun formatPropertyPrice(price: String, marketType: String): String {
    val amount = price.substringBefore('/').trim()
    val match = Regex("(?i)^(?:NPR|Rs\\.?|रु\\.?|रू\\.?)?\\s*([\\d,]+(?:\\.\\d+)?)$")
        .matchEntire(amount)
    val number = match?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull()
    if (number == null) return price.ifBlank { "Price on request" }
    val period = if ('/' in price) {
        "/${price.substringAfter('/').trim()}"
    } else {
        when (marketType.lowercase(Locale.ROOT)) {
            "rent" -> "/month"
            "book" -> "/night"
            else -> ""
        }
    }
    return formatNprAmount(number) + period
}
