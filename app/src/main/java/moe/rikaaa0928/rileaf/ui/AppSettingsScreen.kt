package moe.rikaaa0928.rileaf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.rikaaa0928.rileaf.R
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.LanguageManager
import moe.rikaaa0928.rileaf.data.SupportedLanguage

class AppSettingsViewModel(
    private val configManager: ConfigManager,
    private val languageManager: LanguageManager,
    private val onLanguageChanged: () -> Unit
) : ViewModel() {
    private var _currentLanguage = mutableStateOf(SupportedLanguage.SYSTEM)
    val currentLanguage: State<SupportedLanguage> = _currentLanguage
    
    init {
        loadCurrentLanguage()
    }
    
    private fun loadCurrentLanguage() {
        _currentLanguage.value = languageManager.getCurrentLanguage()
    }
    
    fun selectLanguage(language: SupportedLanguage) {
        if (language != _currentLanguage.value) {
            languageManager.setLanguage(language)
            _currentLanguage.value = language
            onLanguageChanged()
        }
    }
    
    fun getAllLanguages(): List<SupportedLanguage> {
        return languageManager.getAllSupportedLanguages()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    configManager: ConfigManager,
    onNavigateBack: () -> Unit,
    onLanguageChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val languageManager = remember { LanguageManager(configManager) }
    val viewModel: AppSettingsViewModel = viewModel { 
        AppSettingsViewModel(configManager, languageManager, onLanguageChanged) 
    }
    val currentLanguage by viewModel.currentLanguage
    val allLanguages = remember { viewModel.getAllLanguages() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.general_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                LanguageSettingCard(
                    currentLanguage = currentLanguage,
                    allLanguages = allLanguages,
                    onLanguageSelected = { viewModel.selectLanguage(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingCard(
    currentLanguage: SupportedLanguage,
    allLanguages: List<SupportedLanguage>,
    onLanguageSelected: (SupportedLanguage) -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    Card(
        onClick = { showLanguageDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (currentLanguage) {
                            SupportedLanguage.SYSTEM -> stringResource(R.string.language_system)
                            SupportedLanguage.CHINESE -> stringResource(R.string.language_chinese)
                            SupportedLanguage.ENGLISH -> stringResource(R.string.language_english)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                LazyColumn {
                    items(allLanguages) { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (language) {
                                        SupportedLanguage.SYSTEM -> stringResource(R.string.language_system)
                                        SupportedLanguage.CHINESE -> stringResource(R.string.language_chinese)
                                        SupportedLanguage.ENGLISH -> stringResource(R.string.language_english)
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = language.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            RadioButton(
                                selected = language == currentLanguage,
                                onClick = {
                                    onLanguageSelected(language)
                                    showLanguageDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}