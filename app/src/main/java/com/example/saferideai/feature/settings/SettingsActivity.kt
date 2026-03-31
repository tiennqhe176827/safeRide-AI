package com.example.saferideai.feature.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.saferideai.data.settings.AppSettingsRepository
import com.example.saferideai.ui.theme.SafeRideAITheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = AppSettingsRepository(this)

        setContent {
            SafeRideAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        settingsRepository = settingsRepository,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
