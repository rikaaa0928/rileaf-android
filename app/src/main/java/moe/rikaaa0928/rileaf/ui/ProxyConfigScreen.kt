package moe.rikaaa0928.rileaf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.rikaaa0928.rileaf.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.ProxyConfig
import java.util.UUID

class ProxyConfigViewModel(private val configManager: ConfigManager) : ViewModel() {
    private var _proxies = mutableStateOf<List<ProxyConfig>>(emptyList())
    val proxies: State<List<ProxyConfig>> = _proxies
    
    private var _selectedProxyId = mutableStateOf<String?>(null)
    val selectedProxyId: State<String?> = _selectedProxyId
    
    init {
        loadConfig()
    }
    
    private fun loadConfig() {
        val config = configManager.getConfig()
        _proxies.value = config.proxies
        _selectedProxyId.value = config.selectedProxyId
    }
    
    fun addProxy(proxy: ProxyConfig) {
        val newProxies = _proxies.value + proxy
        _proxies.value = newProxies
        saveConfig()
    }
    
    fun updateProxy(proxy: ProxyConfig) {
        _proxies.value = _proxies.value.map { if (it.id == proxy.id) proxy else it }
        saveConfig()
    }
    
    fun deleteProxy(proxyId: String) {
        _proxies.value = _proxies.value.filter { it.id != proxyId }
        if (_selectedProxyId.value == proxyId) {
            _selectedProxyId.value = _proxies.value.firstOrNull()?.id
        }
        saveConfig()
    }
    
    fun selectProxy(proxyId: String) {
        _selectedProxyId.value = proxyId
        saveConfig()
    }
    
    private fun saveConfig() {
        val config = configManager.getConfig().copy(
            proxies = _proxies.value,
            selectedProxyId = _selectedProxyId.value
        )
        configManager.saveConfig(config)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyConfigScreen(
    configManager: ConfigManager,
    isVpnConnected: Boolean = false,
    onNavigateBack: () -> Unit,
    onSwitchProxy: (String) -> Unit = {}
) {
    val viewModel: ProxyConfigViewModel = viewModel { ProxyConfigViewModel(configManager) }
    val proxies by viewModel.proxies
    val selectedProxyId by viewModel.selectedProxyId
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProxy by remember { mutableStateOf<ProxyConfig?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.proxy_config)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_proxy))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(proxies) { proxy ->
                    ProxyConfigItem(
                        proxy = proxy,
                        isSelected = proxy.id == selectedProxyId,
                        isVpnConnected = isVpnConnected,
                        onSelect = { viewModel.selectProxy(proxy.id) },
                        onEdit = { editingProxy = proxy },
                        onDelete = { viewModel.deleteProxy(proxy.id) },
                        onSwitch = { onSwitchProxy(proxy.id) }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        ProxyEditDialog(
            proxy = null,
            onDismiss = { showAddDialog = false },
            onSave = { proxy ->
                viewModel.addProxy(proxy)
                showAddDialog = false
            }
        )
    }
    
    editingProxy?.let { proxy ->
        ProxyEditDialog(
            proxy = proxy,
            onDismiss = { editingProxy = null },
            onSave = { updatedProxy ->
                viewModel.updateProxy(updatedProxy)
                editingProxy = null
            }
        )
    }
}

@Composable
fun ConfirmSwitchDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_switch_title)) },
        text = { Text(stringResource(R.string.confirm_switch_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ProxyConfigItem(
    proxy: ProxyConfig,
    isSelected: Boolean,
    isVpnConnected: Boolean = false,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSwitch: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        ConfirmSwitchDialog(
            onDismiss = { showConfirmDialog = false },
            onConfirm = onSwitch
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = proxy.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${proxy.type.uppercase()} - ${proxy.server}:${proxy.port}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    if (isSelected) {
                        Text(
                            text = stringResource(R.string.selected),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    } else {
                        TextButton(
                            onClick = {
                                if (isVpnConnected) {
                                    showConfirmDialog = true
                                } else {
                                    onSelect()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.select))
                        }
                    }
                    
                    IconButton(
                        onClick = onEdit,
                        enabled = !(isVpnConnected && isSelected)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = !(isVpnConnected && isSelected)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyEditDialog(
    proxy: ProxyConfig?,
    onDismiss: () -> Unit,
    onSave: (ProxyConfig) -> Unit
) {
    var name by remember { mutableStateOf(proxy?.name ?: "") }
    var type by remember { mutableStateOf(proxy?.type ?: "rog") }
    var server by remember { mutableStateOf(proxy?.server ?: "") }
    var port by remember { mutableStateOf(proxy?.port?.toString() ?: "443") }
    var password by remember { mutableStateOf(proxy?.password ?: "") }

    var isNameError by remember { mutableStateOf(false) }
    var isServerError by remember { mutableStateOf(false) }
    var isPortError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        isNameError = name.isBlank()
        isServerError = server.isBlank()
        isPortError = port.toIntOrNull() == null
        return !isNameError && !isServerError && !isPortError
    }

    val typeOptions = listOf("rog", "socks5", "http")
    var expandedType by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (proxy == null) R.string.add_proxy else R.string.edit_proxy)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isNameError = false
                    },
                    label = { Text(stringResource(R.string.proxy_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isNameError,
                    supportingText = { if (isNameError) Text(stringResource(R.string.name_cannot_empty)) }
                )

                Box {
                    OutlinedTextField(
                        value = type.uppercase(),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.proxy_type)) },
                        trailingIcon = {
                            IconButton(onClick = { expandedType = !expandedType }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.select))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        typeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.uppercase()) },
                                onClick = {
                                    type = option
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = server,
                    onValueChange = {
                        server = it
                        isServerError = false
                    },
                    label = { Text(stringResource(R.string.server_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isServerError,
                    supportingText = { if (isServerError) Text(stringResource(R.string.server_cannot_empty)) }
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it
                        isPortError = false
                    },
                    label = { Text(stringResource(R.string.port)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isPortError,
                    supportingText = { if (isPortError) Text(stringResource(R.string.port_invalid)) }
                )

                if (type == "rog") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validate()) {
                        val newProxy = ProxyConfig(
                            id = proxy?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            type = type,
                            server = server,
                            port = port.toInt(),
                            password = if (type == "rog") password else null
                        )
                        onSave(newProxy)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}