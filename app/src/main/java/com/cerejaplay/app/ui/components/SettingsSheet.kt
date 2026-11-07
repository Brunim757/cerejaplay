package com.cerejaplay.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cerejaplay.app.data.model.PreferencesConfig
import com.cerejaplay.app.data.model.UserSettings
import com.cerejaplay.app.data.model.defaultProfiles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    preferences: PreferencesConfig,
    settings: UserSettings,
    legalNotice: String,
    onAudioSelected: (String) -> Unit,
    onQualitySelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onThemeSelected: (String) -> Unit = {},
    onProfileSelected: (String) -> Unit = {},
    onClearHistory: (() -> Unit)? = null,
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
                .padding(bottom = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CerejaWordmark(fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Fechar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Preferências de reprodução",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Os ajustes valem para todas as fontes do catálogo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

            SettingGroup(title = "Áudio") {
                preferences.audioOptions.forEach { option ->
                    FilterChip(
                        selected = settings.audio == option.id,
                        onClick = { onAudioSelected(option.id) },
                        label = { Text(option.label) },
                        colors = chipColors()
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SettingGroup(title = "Qualidade") {
                preferences.qualityOptions.forEach { option ->
                    FilterChip(
                        selected = settings.quality == option,
                        onClick = { onQualitySelected(option) },
                        label = { Text(if (option == "auto") "Auto" else option) },
                        colors = chipColors()
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SettingGroup(title = "Idioma") {
                preferences.languages.forEach { option ->
                    FilterChip(
                        selected = settings.language == option.code,
                        onClick = { onLanguageSelected(option.code) },
                        label = { Text(option.label) },
                        colors = chipColors()
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SettingGroup(title = "Tema") {
                listOf(
                    "dark" to "Escuro",
                    "amoled" to "AMOLED",
                    "light" to "Claro"
                ).forEach { (id, label) ->
                    FilterChip(
                        selected = settings.theme == id,
                        onClick = { onThemeSelected(id) },
                        label = { Text(label) },
                        colors = chipColors()
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SettingGroup(title = "Perfil") {
                defaultProfiles.forEach { option ->
                    FilterChip(
                        selected = settings.profile == option.id,
                        onClick = { onProfileSelected(option.id) },
                        label = { Text(option.name) },
                        colors = chipColors()
                    )
                }
            }

            if (onClearHistory != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = "Limpar histórico de exibição",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (legalNotice.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Aviso",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = legalNotice,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
                )
            }
        }
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
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
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
)
