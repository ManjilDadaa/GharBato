package com.example.gharbato.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.gharbato.model.PropertyListingState
import com.example.gharbato.model.getDefaultImageCategories
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ListingDraft(val state: PropertyListingState, val step: Int)

/** Local, account-scoped drafts. Photos are copied so picker permissions can safely expire. */
class ListingDraftStore(context: Context, userId: String, propertyId: String? = null) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("listing_drafts", Context.MODE_PRIVATE)
    private val key = "$userId:${propertyId ?: "new"}"
    private val imageDirectory = File(appContext.filesDir, "listing-drafts/${UUID.nameUUIDFromBytes(key.toByteArray())}")

    /** Call on an IO dispatcher; a failed save leaves the previous draft intact. */
    fun save(state: PropertyListingState, step: Int): Boolean = runCatching {
        val images = JSONObject()
        state.imageCategories.forEach { category ->
            images.put(category.id, JSONArray(category.images.map(::persistPhoto)))
        }
        val json = JSONObject()
        mapOf(
            "version" to 1, "step" to step,
            "selectedPurpose" to state.selectedPurpose, "selectedPropertyType" to state.selectedPropertyType,
            "title" to state.title, "developer" to state.developer, "price" to state.price,
            "location" to state.location, "area" to state.area, "floor" to state.floor,
            "furnishing" to state.furnishing, "bedrooms" to state.bedrooms, "bathrooms" to state.bathrooms,
            "parking" to state.parking, "petsAllowed" to state.petsAllowed, "description" to state.description,
            "kitchen" to state.kitchen, "totalRooms" to state.totalRooms,
            "latitude" to state.latitude, "longitude" to state.longitude, "hasSelectedLocation" to state.hasSelectedLocation,
            "utilitiesIncluded" to state.utilitiesIncluded, "commission" to state.commission,
            "advancePayment" to state.advancePayment, "securityDeposit" to state.securityDeposit,
            "minimumLease" to state.minimumLease, "availableFrom" to state.availableFrom
        ).forEach { (name, value) -> json.put(name, value) }
        json.put("images", images)
        json.put("amenities", JSONArray(state.amenities))
        preferences.edit().putString(key, json.toString()).commit()
    }.getOrDefault(false)

    fun load(): ListingDraft? = runCatching {
        val raw = preferences.getString(key, null) ?: return null
        val json = JSONObject(raw)
        val defaults = PropertyListingState()
        val images = json.optJSONObject("images")
        val state = PropertyListingState(
            selectedPurpose = json.optString("selectedPurpose"),
            selectedPropertyType = json.optString("selectedPropertyType"),
            title = json.optString("title"), developer = json.optString("developer"), price = json.optString("price"),
            location = json.optString("location"), area = json.optString("area"), floor = json.optString("floor"),
            furnishing = json.optString("furnishing", defaults.furnishing),
            bedrooms = json.optString("bedrooms"), bathrooms = json.optString("bathrooms"),
            parking = json.optBoolean("parking", defaults.parking), petsAllowed = json.optBoolean("petsAllowed"),
            description = json.optString("description"), kitchen = json.optString("kitchen"), totalRooms = json.optString("totalRooms"),
            latitude = json.optDouble("latitude", defaults.latitude), longitude = json.optDouble("longitude", defaults.longitude),
            hasSelectedLocation = json.optBoolean("hasSelectedLocation"),
            imageCategories = getDefaultImageCategories().map { category ->
                category.copy(images = images?.optJSONArray(category.id).strings().toMutableList())
            },
            utilitiesIncluded = json.optString("utilitiesIncluded", defaults.utilitiesIncluded),
            commission = json.optString("commission", defaults.commission),
            advancePayment = json.optString("advancePayment", defaults.advancePayment),
            securityDeposit = json.optString("securityDeposit", defaults.securityDeposit),
            minimumLease = json.optString("minimumLease", defaults.minimumLease),
            availableFrom = json.optString("availableFrom", defaults.availableFrom),
            amenities = json.optJSONArray("amenities").strings()
        )
        ListingDraft(state, json.optInt("step", 1).coerceIn(1, if (state.selectedPurpose == "Rent") 5 else 4))
    }.getOrNull()

    fun clear() {
        preferences.edit().remove(key).commit()
        imageDirectory.deleteRecursively()
    }

    private fun persistPhoto(source: String): String {
        val uri = Uri.parse(source)
        if (uri.scheme == "http" || uri.scheme == "https") return source
        if (uri.scheme == "file" && File(uri.path.orEmpty()).parentFile == imageDirectory) return source
        check(imageDirectory.exists() || imageDirectory.mkdirs())
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(appContext.contentResolver.getType(uri)) ?: "jpg"
        val file = File(imageDirectory, "${UUID.nameUUIDFromBytes(source.toByteArray())}.$extension")
        if (!file.exists() || file.length() == 0L) {
            val temporary = File(imageDirectory, "${file.name}.tmp")
            try {
                val input = requireNotNull(appContext.contentResolver.openInputStream(uri)) { "Photo is no longer available" }
                input.use { stream -> temporary.outputStream().use { stream.copyTo(it) } }
                check(temporary.length() > 0 && temporary.renameTo(file))
            } finally {
                temporary.delete()
            }
        }
        return Uri.fromFile(file).toString()
    }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else List(length()) { getString(it) }
}
