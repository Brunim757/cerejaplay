package com.cerejaplay.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cerejaplay.app.data.model.PreferencesConfig
import com.cerejaplay.app.data.model.UserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    preferences: PreferencesConfig,
    settings: UserSettings,
    legalNotice: String,
    onAudioSelected: (String) -> Unit,
    onQualitySelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Preferências de reprodução",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            SettingRow(title = "Áudio") {
                preferences.audioOptions.forEach { option ->
                    FilterChip(
                        selected = settings.audio == option.id,
                        onClick = { onAudioSelected(option.id) },
                        label = { Text(option.label) },
                        colors = chipColors()
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            SettingRow(title = "Qualidade") {
                preferences.qualityOptions.forEach { option ->
                    FilterChip(
                        selected = settings.quality == option,
                        onClick = { onQualitySelected(option) },
                        label = { Text(if (option == "auto") "Auto" else option) },
                        colors = chipColors()
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            SettingRow(title = "Idioma") {
                preferences.languages.forEach { option ->
                    FilterChip(
                        selected = settings.language == option.code,
                        onClick = { onLanguageSelected(option.code) },
                        label = { Text(option.label) },
                        colors = chipColors()
                    )
                }
            }

            if (legalNotice.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = legalNotice,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
)
