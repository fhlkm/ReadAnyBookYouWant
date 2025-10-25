package com.book.rabyw.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.book.rabyw.domain.models.Language
import com.book.rabyw.domain.models.TranslationMode

@Composable
fun TranslationControls(
    sourceLanguage: Language,
    targetLanguage: Language,
    translationMode: TranslationMode,
    onSourceLanguageChanged: (Language) -> Unit,
    onTargetLanguageChanged: (Language) -> Unit,
    onTranslationModeChanged: (TranslationMode) -> Unit,
    onTranslate: () -> Unit,
    isTranslating: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Translation Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Language Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source Language
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "From:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LanguageSelector(
                        selectedLanguage = sourceLanguage,
                        onLanguageSelected = onSourceLanguageChanged,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Target Language
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "To:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LanguageSelector(
                        selectedLanguage = targetLanguage,
                        onLanguageSelected = onTargetLanguageChanged,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Translation Mode Selection
            Text(
                text = "Translation Mode:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TranslationMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = translationMode == mode,
                                onClick = { onTranslationModeChanged(mode) }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = translationMode == mode,
                            onClick = { onTranslationModeChanged(mode) }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (mode) {
                                TranslationMode.FAST -> "Fast (Offline)"
                                TranslationMode.ACCURATE -> "Accurate (Online)"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Translate Button
            Button(
                onClick = onTranslate,
                enabled = !isTranslating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Translate")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    selectedLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLanguage.displayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Language.values().forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}
