package com.example.gharbato.utils

/** Preserve category and photo order when existing URLs are mixed with new uploads. */
internal fun resolveListingImages(
    categories: Map<String, List<String>>,
    uploadedUrls: List<String>
): Map<String, List<String>> {
    fun remote(source: String) = source.startsWith("https://") || source.startsWith("http://")
    val expected = categories.values.flatten().count { !remote(it) }
    require(uploadedUrls.size == expected && uploadedUrls.all { it.isNotBlank() }) {
        "Every selected photo must finish uploading before saving the listing"
    }
    val uploads = uploadedUrls.iterator()
    return categories.filterValues { it.isNotEmpty() }.mapValues { (_, sources) ->
        sources.map { if (remote(it)) it else uploads.next() }
    }
}
