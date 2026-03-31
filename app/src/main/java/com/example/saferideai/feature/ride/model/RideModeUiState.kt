package com.example.saferideai.feature.ride.model

data class RideModeUiState(
    val isListening: Boolean = false,
    val statusText: String = "Dang khoi tao Ride Mode...",
    val heardText: String = "Chua co am thanh duoc nhan dien.",
    val emergencyKeyword: String = "",
    val primaryPhone: String = "",
    val tripId: String = "",
    val latestLocationText: String = "Chua co vi tri moi.",
    val firebaseStatusText: String = "Chua gui du lieu len Firebase.",
    val smsStatusText: String = "Chua gui SMS khan cap."
)
