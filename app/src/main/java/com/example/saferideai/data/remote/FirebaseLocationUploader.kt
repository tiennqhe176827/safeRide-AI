package com.example.saferideai.data.remote

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class FirebaseLocationUploader(
    private val databaseUrl: String = DEFAULT_DATABASE_URL
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun updateRideLocation(
        tripId: String,
        latitude: Double,
        longitude: Double,
        emergencyKeyword: String,
        primaryPhone: String,
        onResult: ((UploadResult) -> Unit)? = null
    ) {
        val payload = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("emergencyKeyword", emergencyKeyword)
            put("primaryPhone", primaryPhone)
            put("updatedAt", System.currentTimeMillis())
            put("status", "active")
        }

        sendPatch("ride_locations/$tripId.json", payload, onResult)
    }

    fun markEmergency(
        tripId: String,
        latitude: Double?,
        longitude: Double?,
        emergencyKeyword: String,
        primaryPhone: String,
        onResult: ((UploadResult) -> Unit)? = null
    ) {
        val payload = JSONObject().apply {
            put("emergencyKeyword", emergencyKeyword)
            put("primaryPhone", primaryPhone)
            put("updatedAt", System.currentTimeMillis())
            put("status", "emergency")

            if (latitude != null && longitude != null) {
                put("latitude", latitude)
                put("longitude", longitude)
            }
        }

        sendPatch("ride_locations/$tripId.json", payload, onResult)
    }

    private fun sendPatch(
        path: String,
        payload: JSONObject,
        onResult: ((UploadResult) -> Unit)?
    ) {
        executor.execute {
            val connection = (URL("$databaseUrl/$path").openConnection() as HttpURLConnection)
            val result = try {
                connection.requestMethod = "PATCH"
                connection.doOutput = true
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                }

                val code = connection.responseCode
                val body = runCatching {
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }.getOrDefault("")

                UploadResult(
                    success = code in 200..299,
                    code = code,
                    message = if (body.isBlank()) "Empty response" else body
                )
            } catch (exception: Exception) {
                UploadResult(
                    success = false,
                    code = -1,
                    message = exception.message ?: "Unknown network error"
                )
            } finally {
                connection.disconnect()
            }

            onResult?.let { callback ->
                mainHandler.post { callback(result) }
            }
        }
    }

    companion object {
        private const val DEFAULT_DATABASE_URL =
            "https://saferide-ai-61791-default-rtdb.asia-southeast1.firebasedatabase.app"
    }
}
