package com.example.saferideai.feature.home

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.saferideai.core.permissions.hasPermission
import com.example.saferideai.data.settings.AppSettingsRepository
import com.example.saferideai.feature.ride.RideModeActivity
import com.example.saferideai.feature.settings.SettingsActivity

private const val PREFS_NAME = "safe_ride_permissions"
private const val KEY_REQUESTED_ON_FIRST_OPEN = "requested_on_first_open"

private val requiredPermissions = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.ACCESS_FINE_LOCATION
)

@Composable
fun HomeScreenRoute() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsRepository = remember { AppSettingsRepository(context) }
    val preferences = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var emergencyKeyword by remember { mutableStateOf(settingsRepository.getEmergencyKeyword()) }
    var familyPhones by remember { mutableStateOf(settingsRepository.getFamilyPhones()) }
    var micGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.RECORD_AUDIO)) }
    var locationGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        micGranted = result[Manifest.permission.RECORD_AUDIO] == true ||
            context.hasPermission(Manifest.permission.RECORD_AUDIO)
        locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(Unit) {
        val firstOpenHandled = preferences.getBoolean(KEY_REQUESTED_ON_FIRST_OPEN, false)
        if (!firstOpenHandled) {
            preferences.edit().putBoolean(KEY_REQUESTED_ON_FIRST_OPEN, true).apply()
            if (!micGranted || !locationGranted) {
                permissionLauncher.launch(requiredPermissions)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                emergencyKeyword = settingsRepository.getEmergencyKeyword()
                familyPhones = settingsRepository.getFamilyPhones()
                micGranted = context.hasPermission(Manifest.permission.RECORD_AUDIO)
                locationGranted = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    HomeScreen(
        emergencyKeyword = emergencyKeyword,
        familyPhones = familyPhones,
        micGranted = micGranted,
        locationGranted = locationGranted,
        onNavigateToSettings = {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        },
        onPrimaryAction = {
            if (micGranted && locationGranted) {
                context.startActivity(Intent(context, RideModeActivity::class.java))
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
        },
        onRequestPermissions = {
            permissionLauncher.launch(requiredPermissions)
        }
    )
}

@Composable
fun HomeScreen(
    emergencyKeyword: String,
    familyPhones: List<String>,
    micGranted: Boolean,
    locationGranted: Boolean,
    onNavigateToSettings: () -> Unit,
    onPrimaryAction: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            HeaderSection(onNavigateToSettings)
            HeroSection(
                allPermissionsGranted = micGranted && locationGranted,
                emergencyKeyword = emergencyKeyword,
                familyPhones = familyPhones,
                onPrimaryAction = onPrimaryAction
            )
            QuickStatusSection(micGranted, locationGranted)
            PermissionNotice(
                micGranted = micGranted,
                locationGranted = locationGranted,
                onRequestPermissions = onRequestPermissions
            )
        }
    }
}

@Composable
private fun HeaderSection(onNavigateToSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("SafeRide AI", style = MaterialTheme.typography.headlineMedium)
            Text(
                "An toan cho moi chuyen di.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Cai dat",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HeroSection(
    allPermissionsGranted: Boolean,
    emergencyKeyword: String,
    familyPhones: List<String>,
    onPrimaryAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = if (allPermissionsGranted) "San sang len duong" else "Can cap quyen de bat dau",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (allPermissionsGranted) {
                    "Ride Mode da san sang theo doi hanh trinh va ho tro canh bao trong truong hop bat thuong."
                } else {
                    "App can quyen micro va dinh vi de ghi nhan hanh trinh, phat hien bat thuong va gui canh bao."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tu khoa hien tai: \"$emergencyKeyword\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (familyPhones.isEmpty()) {
                    "So nguoi than: chua cai dat"
                } else {
                    "So nguoi than: ${familyPhones.joinToString()}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (allPermissionsGranted) "Bat dau Ride Mode" else "Cap quyen ngay",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun QuickStatusSection(
    micGranted: Boolean,
    locationGranted: Boolean
) {
    Text("Trang thai nhanh", style = MaterialTheme.typography.titleLarge)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusCard(
            modifier = Modifier.weight(1f),
            title = "Micro",
            value = if (micGranted) "Da cap quyen" else "Chua cap quyen",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        StatusCard(
            modifier = Modifier.weight(1f),
            title = "GPS",
            value = if (locationGranted) "Da cap quyen" else "Chua cap quyen",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionNotice(
    micGranted: Boolean,
    locationGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    if (micGranted && locationGranted) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Quyen can thiet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Micro dung de lang nghe tinh huong khan cap. Dinh vi dung de theo doi vi tri hien tai va ho tro canh bao.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cap quyen micro va dinh vi")
            }
        }
    }
}
