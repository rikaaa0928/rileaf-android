package moe.rikaaa0928.rileaf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.rikaaa0928.rileaf.R
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.LanguageManager
import moe.rikaaa0928.rileaf.data.SupportedLanguage

class LanguageSettingsViewModel(private val languageManager: LanguageManager) : ViewModel() {
    private var _currentLanguage = mutableStateOf(SupportedLanguage.SYSTEM)
    val currentLanguage: State<SupportedLanguage> = _currentLanguage
    
    private var _showRestartDialog = mutableStateOf(false)
    val showRestartDialog: State<Boolean> = _showRestartDialog
    
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
            _showRestartDialog.value = true
        }
    }
    
    fun dismissRestartDialog() {
        _showRestartDialog.value = false
    }
    
    fun getAllLanguages(): List<SupportedLanguage> {
        return languageManager.getAllSupportedLanguages()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    configManager: ConfigManager,
    onNavigateBack: () -> Unit
) {
    val languageManager = remember { LanguageManager(configManager) }
    val viewModel: LanguageSettingsViewModel = viewModel { LanguageSettingsViewModel(languageManager) }
    val currentLanguage by viewModel.currentLanguage
    val showRestartDialog by viewModel.showRestartDialog
    val allLanguages = remember { viewModel.getAllLanguages() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.language_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 当前语言显示
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.current_language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (currentLanguage) {
                            SupportedLanguage.SYSTEM -> stringResource(R.string.language_system)
                            SupportedLanguage.CHINESE -> stringResource(R.string.language_chinese)
                            SupportedLanguage.ENGLISH -> stringResource(R.string.language_english)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 语言选择列表
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allLanguages) { language ->
                    LanguageOptionCard(
                        language = language,
                        isSelected = language == currentLanguage,
                        onSelect = { viewModel.selectLanguage(language) }
                    )
                }
            }
        }
    }
    
    // 重启提示对话框
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestartDialog() },
            title = { Text(stringResource(R.string.language_settings)) },
            text = { Text(stringResource(R.string.restart_required)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissRestartDialog() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun LanguageOptionCard(
    language: SupportedLanguage,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}