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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                title = { Text("代理配置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加代理")
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
                            text = "VPN 已连接，无法更改当前使用的代理配置",
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
                            text = "已选择",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    } else {
                        TextButton(
                            onClick = onSelect,
                            enabled = !isVpnConnected
                        ) {
                            Text("选择")
                        }
                    }
                    
                    IconButton(
                        onClick = onEdit,
                        enabled = !(isVpnConnected && isSelected)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = !(isVpnConnected && isSelected)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
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
        title = { Text(if (proxy == null) "添加代理" else "编辑代理") },
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
                    label = { Text("代理名称") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isNameError,
                    supportingText = { if (isNameError) Text("名称不能为空") }
                )

                Box {
                    OutlinedTextField(
                        value = type.uppercase(),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("代理类型") },
                        trailingIcon = {
                            IconButton(onClick = { expandedType = !expandedType }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "选择")
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
                    label = { Text("服务器地址") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isServerError,
                    supportingText = { if (isServerError) Text("服务器地址不能为空") }
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it
                        isPortError = false
                    },
                    label = { Text("端口") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isPortError,
                    supportingText = { if (isPortError) Text("端口必须是有效的数字") }
                )

                if (type == "rog") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
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
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}