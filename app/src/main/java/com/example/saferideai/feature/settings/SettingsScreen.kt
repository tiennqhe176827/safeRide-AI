package com.example.saferideai.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.saferideai.data.settings.AppSettingsRepository

@Composable
fun SettingsScreen(
    settingsRepository: AppSettingsRepository,
    onBack: () -> Unit
) {
    var emergencyKeyword by remember {
        mutableStateOf(settingsRepository.getEmergencyKeyword())
    }
    var familyPhones by remember {
        mutableStateOf(settingsRepository.getFamilyPhones().ifEmpty { listOf("") })
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Quay lai"
                    )
                }

                Column {
                    Text("Cai dat", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Sua cau hinh dung chung cho ung dung.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Tu khoa khan cap", style = MaterialTheme.typography.titleLarge)

                    OutlinedTextField(
                        value = emergencyKeyword,
                        onValueChange = { emergencyKeyword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nhap tu khoa") },
                        singleLine = true
                    )

                    Text("So dien thoai nguoi than", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Them nhieu so, moi so mot o rieng.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    familyPhones.forEachIndexed { index, phone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { value ->
                                    familyPhones = familyPhones.toMutableList().also { phones ->
                                        phones[index] = value
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("So ${index + 1}") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            IconButton(
                                onClick = {
                                    familyPhones = familyPhones.toMutableList().also { phones ->
                                        if (phones.size > 1) {
                                            phones.removeAt(index)
                                        } else {
                                            phones[0] = ""
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Xoa so"
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { familyPhones = familyPhones + "" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("Them so dien thoai", modifier = Modifier.padding(start = 8.dp))
                    }

                    Button(
                        onClick = {
                            settingsRepository.setEmergencyKeyword(emergencyKeyword)
                            settingsRepository.setFamilyPhones(familyPhones)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Luu cau hinh")
                    }
                }
            }
        }
    }
}
