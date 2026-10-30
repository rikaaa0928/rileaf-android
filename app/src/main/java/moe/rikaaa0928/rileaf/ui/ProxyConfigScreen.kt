package moe.rikaaa0928.rileaf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
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
    onNavigateBack: () -> Unit
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
            // VPN 连接状态提示
            if (isVpnConnected) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.vpn_connected_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
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
                        onDelete = { viewModel.deleteProxy(proxy.id) }
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
fun ProxyConfigItem(
    proxy: ProxyConfig,
    isSelected: Boolean,
    isVpnConnected: Boolean = false,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                            onClick = onSelect,
                            enabled = !isVpnConnected
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
    var customConnector by remember { mutableStateOf(proxy?.customConnector ?: true) }
    var keepAlive by remember { mutableStateOf(proxy?.keepAlive ?: true) }
    var keepAliveIntervalSecs by remember {
        mutableStateOf((proxy?.keepAliveIntervalSecs ?: 30).toString())
    }
    var keepAliveTimeoutSecs by remember {
        mutableStateOf((proxy?.keepAliveTimeoutSecs ?: 20).toString())
    }
    var keepAliveWhileIdle by remember { mutableStateOf(proxy?.keepAliveWhileIdle ?: false) }

    var isNameError by remember { mutableStateOf(false) }
    var isServerError by remember { mutableStateOf(false) }
    var isPortError by remember { mutableStateOf(false) }
    var isKeepAliveIntervalError by remember { mutableStateOf(false) }
    var isKeepAliveTimeoutError by remember { mutableStateOf(false) }

    val isRogFamily = type == "rog" || type == "rog-h3"

    fun validate(): Boolean {
        isNameError = name.isBlank()
        isServerError = server.isBlank()
        isPortError = port.toIntOrNull() == null
        isKeepAliveIntervalError = isRogFamily && keepAlive &&
            ((keepAliveIntervalSecs.toIntOrNull() ?: 0) <= 0)
        isKeepAliveTimeoutError = isRogFamily && keepAlive &&
            ((keepAliveTimeoutSecs.toIntOrNull() ?: 0) <= 0)
        return !isNameError &&
            !isServerError &&
            !isPortError &&
            !isKeepAliveIntervalError &&
            !isKeepAliveTimeoutError
    }

    val typeOptions = listOf("rog", "rog-h3", "socks5", "http")
    var expandedType by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (proxy == null) R.string.add_proxy else R.string.edit_proxy)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
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
                                    if (type != option && option == "rog-h3") {
                                        keepAlive = false
                                        keepAliveTimeoutSecs = "45"
                                    }
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

                if (isRogFamily) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.rog_transport_options),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.custom_connector))
                            Text(
                                text = stringResource(R.string.custom_connector_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = customConnector,
                            onCheckedChange = { customConnector = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.keep_alive))
                            Text(
                                text = stringResource(R.string.keep_alive_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = keepAlive,
                            onCheckedChange = { keepAlive = it }
                        )
                    }

                    if (keepAlive) {
                        OutlinedTextField(
                            value = keepAliveIntervalSecs,
                            onValueChange = {
                                keepAliveIntervalSecs = it
                                isKeepAliveIntervalError = false
                            },
                            label = { Text(stringResource(R.string.keep_alive_interval_secs)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            isError = isKeepAliveIntervalError,
                            supportingText = {
                                if (isKeepAliveIntervalError) {
                                    Text(stringResource(R.string.positive_number_required))
                                }
                            }
                        )

                        OutlinedTextField(
                            value = keepAliveTimeoutSecs,
                            onValueChange = {
                                keepAliveTimeoutSecs = it
                                isKeepAliveTimeoutError = false
                            },
                            label = { Text(stringResource(R.string.keep_alive_timeout_secs)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            isError = isKeepAliveTimeoutError,
                            supportingText = {
                                if (isKeepAliveTimeoutError) {
                                    Text(stringResource(R.string.positive_number_required))
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.keep_alive_while_idle))
                                Text(
                                    text = stringResource(R.string.keep_alive_while_idle_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = keepAliveWhileIdle,
                                onCheckedChange = { keepAliveWhileIdle = it }
                            )
                        }
                    }
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
                            password = if (isRogFamily) password else null,
                            customConnector = customConnector,
                            keepAlive = keepAlive,
                            keepAliveIntervalSecs = keepAliveIntervalSecs.toIntOrNull() ?: 30,
                            keepAliveTimeoutSecs = keepAliveTimeoutSecs.toIntOrNull() ?: 20,
                            keepAliveWhileIdle = keepAliveWhileIdle
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
