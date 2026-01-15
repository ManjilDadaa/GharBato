package com.example.gharbato.model

import com.google.firebase.database.PropertyName

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,

    val propertyId: Int = 0,
    val propertyTitle: String = "",
    val propertyPrice: String = "",
    val propertyImage: String = "",
    val propertyLocation: String = "",
    val propertyBedrooms: Int = 0,
    val propertyBathrooms: Int = 0
) {
    val hasPropertyCard: Boolean
        get() = propertyId > 0 && propertyTitle.isNotEmpty()
}
