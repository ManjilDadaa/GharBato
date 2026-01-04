package com.example.gharbato.model

data class ListingValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
)