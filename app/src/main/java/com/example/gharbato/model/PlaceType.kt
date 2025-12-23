package com.example.gharbato.data.model

enum class PlaceType {
    SCHOOL,
    HOSPITAL,
    STORE,
    PARK,
    RESTAURANT,
    TRANSPORT;

    fun getDisplayName(): String = when (this) {
        SCHOOL -> "Schools"
        HOSPITAL -> "Hospitals"
        STORE -> "Stores"
        PARK -> "Parks"
        RESTAURANT -> "Restaurants"
        TRANSPORT -> "Transport"
    }
}
