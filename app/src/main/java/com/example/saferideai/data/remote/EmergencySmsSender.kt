package com.example.saferideai.data.remote

import android.telephony.SmsManager
import java.net.URLEncoder

class EmergencySmsSender {

    fun sendEmergencyLocation(
        phones: List<String>,
        tripId: String,
        latitude: Double?,
        longitude: Double?,
        emergencyKeyword: String
    ): UploadResult {
        val normalizedPhones = phones
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (normalizedPhones.isEmpty()) {
            return UploadResult(
                success = false,
                code = -1,
                message = "Khong co so dien thoai khan cap."
            )
        }

        val message = buildMessage(
            tripId = tripId,
            latitude = latitude,
            longitude = longitude,
            emergencyKeyword = emergencyKeyword
        )

        val smsManager = SmsManager.getDefault()

        return try {
            normalizedPhones.forEach { phone ->
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(phone, null, message, null, null)
                }
            }

            UploadResult(
                success = true,
                code = normalizedPhones.size,
                message = "Da gui den ${normalizedPhones.size} lien he."
            )
        } catch (exception: Exception) {
            UploadResult(
                success = false,
                code = -1,
                message = exception.message ?: "Khong gui duoc SMS."
            )
        }
    }

    private fun buildMessage(
        tripId: String,
        latitude: Double?,
        longitude: Double?,
        emergencyKeyword: String
    ): String {
        return buildString {
            append("CANH BAO SafeRide AI. ")
            append("Nguoi dung vua kich hoat che do khan cap. ")
            append("Tu khoa: ").append(emergencyKeyword).append(". ")
            append("Trip: ").append(tripId).append(". ")

            if (latitude != null && longitude != null) {
                append("Vi tri: ").append(latitude).append(", ").append(longitude).append(". ")
                append("Ban do: ").append(buildMapsUrl(latitude, longitude))
            } else {
                append("Chua lay duoc GPS chinh xac.")
            }
        }
    }

    private fun buildMapsUrl(latitude: Double, longitude: Double): String {
        val query = URLEncoder.encode("$latitude,$longitude", Charsets.UTF_8.name())
        return "https://www.google.com/maps/search/?api=1&query=$query"
    }
}
