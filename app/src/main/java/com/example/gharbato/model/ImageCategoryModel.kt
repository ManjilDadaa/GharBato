package com.example.gharbato.model

import android.net.Uri

data class ImageCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int,
    val isRequired: Boolean,
    val maxImages: Int,
    val images: MutableList<Uri> = mutableListOf()
)
