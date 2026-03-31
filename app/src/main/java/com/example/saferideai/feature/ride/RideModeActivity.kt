package com.example.saferideai.feature.ride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.saferideai.core.permissions.hasPermission
import com.example.saferideai.data.remote.EmergencySmsSender
import com.example.saferideai.data.remote.FirebaseLocationUploader
import com.example.saferideai.data.settings.AppSettingsRepository
import com.example.saferideai.feature.ride.location.RideLocationTracker
import com.example.saferideai.feature.ride.model.RideModeUiState
import com.example.saferideai.feature.ride.speech.SpeechKeywordDetector
import com.example.saferideai.feature.ride.ui.RideModeScreen
import com.example.saferideai.ui.theme.SafeRideAITheme

class RideModeActivity : ComponentActivity() {

    private val tripId = "ride_${System.currentTimeMillis()}"
    private val settingsRepository by lazy { AppSettingsRepository(this) }
    private val firebaseLocationUploader by lazy { FirebaseLocationUploader() }
    private val emergencySmsSender by lazy { EmergencySmsSender() }

    private var latestLatitude: Double? = null
    private var latestLongitude: Double? = null
    private var hasTriggeredEmergency = false
    private var pendingEmergencyPhones: List<String> = emptyList()

    private var uiState by mutableStateOf(RideModeUiState(tripId = tripId))

    private val speechKeywordDetector by lazy {
        SpeechKeywordDetector(
            context = this,
            onStatusChanged = { status -> uiState = uiState.copy(statusText = status) },
            onHeardText = { heard -> uiState = uiState.copy(heardText = heard) },
            onKeywordDetected = { triggerEmergencyCall() }
        )
    }

    private val rideLocationTracker by lazy {
        RideLocationTracker(
            context = this,
            onLocationUpdated = { location ->
                latestLatitude = location.latitude
                latestLongitude = location.longitude
                uiState = uiState.copy(
                    latestLocationText = "Lat ${location.latitude}, Lng ${location.longitude}"
                )
                firebaseLocationUploader.updateRideLocation(
                    tripId = tripId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    emergencyKeyword = uiState.emergencyKeyword,
                    primaryPhone = uiState.primaryPhone,
                    onResult = { result ->
                        uiState = uiState.copy(
                            firebaseStatusText = if (result.success) {
                                "Firebase OK (${result.code})"
                            } else {
                                "Firebase loi (${result.code}): ${result.message}"
                            }
                        )
                    }
                )
            },
            onPermissionMissing = {
                uiState = uiState.copy(latestLocationText = "Chua duoc cap quyen dinh vi.")
            }
        )
    }

    private val callPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        uiState = if (granted) {
            callPrimaryContact()
            uiState
        } else {
            uiState.copy(statusText = "Chua duoc cap quyen goi dien. Khong the goi khan cap.")
        }
    }

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            sendEmergencySmsToContacts(pendingEmergencyPhones)
        } else {
            uiState = uiState.copy(
                smsStatusText = "Chua duoc cap quyen SEND_SMS. Khong gui duoc dinh vi qua SMS."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshConfig()

        setContent {
            SafeRideAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RideModeScreen(
                        uiState = uiState,
                        onBack = { finish() },
                        onRestartListening = {
                            hasTriggeredEmergency = false
                            startRideMode()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshConfig()
        if (!hasTriggeredEmergency) {
            startRideMode()
        }
    }

    override fun onPause() {
        super.onPause()
        stopRideMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechKeywordDetector.destroy()
    }

    private fun refreshConfig() {
        uiState = uiState.copy(
            emergencyKeyword = settingsRepository.getEmergencyKeyword(),
            primaryPhone = settingsRepository.getFamilyPhones().firstOrNull().orEmpty()
        )
    }

    private fun startRideMode() {
        if (hasTriggeredEmergency) {
            uiState = uiState.copy(statusText = "Dang o trang thai khan cap. Nhan 'Nghe lai ngay' de khoi dong lai.")
            return
        }

        if (!speechKeywordDetector.isRecognitionAvailable()) {
            uiState = uiState.copy(statusText = "May khong ho tro nhan dien giong noi.")
            return
        }

        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            uiState = uiState.copy(statusText = "Can quyen micro de bat Ride Mode.")
            return
        }

        hasTriggeredEmergency = false
        uiState = uiState.copy(isListening = true)
        speechKeywordDetector.start(uiState.emergencyKeyword)
        rideLocationTracker.start()
    }

    private fun stopRideMode() {
        uiState = uiState.copy(isListening = false)
        speechKeywordDetector.stop()
        rideLocationTracker.stop()
    }

    private fun triggerEmergencyCall() {
        hasTriggeredEmergency = true
        stopRideMode()

        val emergencyPhones = settingsRepository.getFamilyPhones()
        if (emergencyPhones.isEmpty()) {
            uiState = uiState.copy(statusText = "Chua co so nguoi than de goi khan cap.")
            return
        }

        firebaseLocationUploader.markEmergency(
            tripId = tripId,
            latitude = latestLatitude,
            longitude = latestLongitude,
            emergencyKeyword = uiState.emergencyKeyword,
            primaryPhone = uiState.primaryPhone,
            onResult = { result ->
                uiState = uiState.copy(
                    firebaseStatusText = if (result.success) {
                        "Emergency OK (${result.code})"
                    } else {
                        "Emergency loi (${result.code}): ${result.message}"
                    }
                )
            }
        )

        sendEmergencySmsOrRequestPermission(emergencyPhones)

        uiState = uiState.copy(statusText = "Da phat hien tu khoa. Dang goi ${uiState.primaryPhone}")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            callPrimaryContact()
        } else {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun callPrimaryContact() {
        val sanitizedPhone = PhoneNumberUtils.normalizeNumber(uiState.primaryPhone)
        if (sanitizedPhone.isBlank()) {
            uiState = uiState.copy(statusText = "So dien thoai nguoi than khong hop le.")
            return
        }

        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$sanitizedPhone"))
        try {
            startActivity(intent)
        } catch (_: SecurityException) {
            uiState = uiState.copy(statusText = "Khong the goi vi thieu quyen CALL_PHONE.")
        }
    }

    private fun sendEmergencySmsOrRequestPermission(phones: List<String>) {
        pendingEmergencyPhones = phones

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            sendEmergencySmsToContacts(phones)
        } else {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun sendEmergencySmsToContacts(phones: List<String>) {
        if (phones.isEmpty()) {
            uiState = uiState.copy(smsStatusText = "Khong co lien he nao de gui SMS.")
            return
        }

        val result = emergencySmsSender.sendEmergencyLocation(
            phones = phones,
            tripId = tripId,
            latitude = latestLatitude,
            longitude = latestLongitude,
            emergencyKeyword = uiState.emergencyKeyword
        )

        uiState = uiState.copy(
            smsStatusText = if (result.success) {
                "SMS OK: ${result.message}"
            } else {
                "SMS loi: ${result.message}"
            }
        )
    }
}
