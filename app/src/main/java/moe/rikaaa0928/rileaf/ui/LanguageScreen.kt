package moe.rikaaa0928.rileaf.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import moe.rikaaa0928.rileaf.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onNavigateBack: () -> Unit
) {
    val locales = mapOf(
        "en" to "English",
        "zh" to "简体中文"
    )
    var currentLanguage by remember {
        mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "en" })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.language_selection_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            locales.forEach { (tag, name) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (currentLanguage == tag),
                            onClick = {
                                val localeList = LocaleListCompat.forLanguageTags(tag)
                                AppCompatDelegate.setApplicationLocales(localeList)
                                currentLanguage = tag
                            }
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (currentLanguage == tag),
                        onClick = null
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}
