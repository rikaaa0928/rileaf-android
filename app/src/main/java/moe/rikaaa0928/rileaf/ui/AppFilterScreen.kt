package moe.rikaaa0928.rileaf.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import moe.rikaaa0928.rileaf.R
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.AppFilterConfig
import moe.rikaaa0928.rileaf.data.ApplicationInfo

enum class AppFilterType {
    ALL,        // 所有应用
    USER,       // 非系统应用
    SYSTEM      // 系统应用
}

class AppFilterViewModel(private val configManager: ConfigManager) : ViewModel() {
    private var _appFilterConfig = mutableStateOf(AppFilterConfig())
    val appFilterConfig: State<AppFilterConfig> = _appFilterConfig
    
    private var _installedApps = mutableStateOf<List<ApplicationInfo>>(emptyList())
    val installedApps: State<List<ApplicationInfo>> = _installedApps
    
    private var _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    
    private var _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery
    
    private var _filterType = mutableStateOf(AppFilterType.ALL)
    val filterType: State<AppFilterType> = _filterType
    
    val filteredApps: State<List<ApplicationInfo>> = derivedStateOf {
        val apps = _installedApps.value
        
        // 按应用类型筛选
        val typeFilteredApps = when (_filterType.value) {
            AppFilterType.ALL -> apps
            AppFilterType.USER -> apps.filter { !it.isSystemApp }
            AppFilterType.SYSTEM -> apps.filter { it.isSystemApp }
        }
        
        // 按搜索关键词筛选
        if (_searchQuery.value.isBlank()) {
            typeFilteredApps
        } else {
            typeFilteredApps.filter { app ->
                app.appName.contains(_searchQuery.value, ignoreCase = true) ||
                app.packageName.contains(_searchQuery.value, ignoreCase = true)
            }
        }
    }
    
    init {
        loadConfig()
        loadInstalledApps()
    }
    
    private fun loadConfig() {
        _appFilterConfig.value = configManager.getConfig().appFilterConfig
    }
    
    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _installedApps.value = configManager.getInstalledApps()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun updateFilterType(filterType: AppFilterType) {
        _filterType.value = filterType
    }
    
    fun toggleFilterMode() {
        val newConfig = _appFilterConfig.value.copy(
            isWhitelistMode = !_appFilterConfig.value.isWhitelistMode
        )
        _appFilterConfig.value = newConfig
        saveConfig()
    }
    
    fun toggleAppSelection(packageName: String) {
        val currentSelection = _appFilterConfig.value.selectedApps
        val newSelection = if (currentSelection.contains(packageName)) {
            currentSelection - packageName
        } else {
            currentSelection + packageName
        }
        
        val newConfig = _appFilterConfig.value.copy(selectedApps = newSelection)
        _appFilterConfig.value = newConfig
        saveConfig()
    }
    
    private fun saveConfig() {
        val config = configManager.getConfig().copy(appFilterConfig = _appFilterConfig.value)
        configManager.saveConfig(config)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterScreen(
    configManager: ConfigManager,
    onNavigateBack: () -> Unit
) {
    val viewModel: AppFilterViewModel = viewModel { AppFilterViewModel(configManager) }
    val appFilterConfig by viewModel.appFilterConfig
    val installedApps by viewModel.installedApps
    val filteredApps by viewModel.filteredApps
    val isLoading by viewModel.isLoading
    val searchQuery by viewModel.searchQuery
    val filterType by viewModel.filterType
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_filter)) },
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
        ) {
            // 过滤模式选择
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.filter_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (appFilterConfig.isWhitelistMode) stringResource(R.string.whitelist_mode) else stringResource(R.string.blacklist_mode),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (appFilterConfig.isWhitelistMode) 
                                    stringResource(R.string.whitelist_desc) 
                                else 
                                    stringResource(R.string.blacklist_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = appFilterConfig.isWhitelistMode,
                            onCheckedChange = { viewModel.toggleFilterMode() }
                        )
                    }
                }
            }
            
            // 应用类型筛选
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_type),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFilterType.values().forEach { type ->
                            val isSelected = filterType == type
                            val count = when (type) {
                                AppFilterType.ALL -> installedApps.size
                                AppFilterType.USER -> installedApps.count { !it.isSystemApp }
                                AppFilterType.SYSTEM -> installedApps.count { it.isSystemApp }
                            }
                            val label = when (type) {
                                AppFilterType.ALL -> stringResource(R.string.all_apps, count)
                                AppFilterType.USER -> stringResource(R.string.user_apps, count)
                                AppFilterType.SYSTEM -> stringResource(R.string.system_apps, count)
                            }
                            
                            FilterChip(
                                onClick = { viewModel.updateFilterType(type) },
                                label = { Text(label) },
                                selected = isSelected,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text(stringResource(R.string.search_apps)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 统计信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.showing_apps, filteredApps.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.selected_apps, appFilterConfig.selectedApps.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 应用列表
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredApps) { app ->
                        AppFilterItem(
                            app = app,
                            isSelected = appFilterConfig.selectedApps.contains(app.packageName),
                            onToggle = { viewModel.toggleAppSelection(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterItem(
    app: ApplicationInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    val appIcon = try {
        val drawable = packageManager.getApplicationIcon(app.packageName)
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        // Fallback to a default icon if needed
        null
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null, // decorative
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (app.isSystemApp) {
                    Text(
                        text = stringResource(R.string.system_app),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Checkbox(
                checked = isSelected,
                onCheckedChange = null // a null lambda makes the checkbox read-only
            )
        }
    }
}