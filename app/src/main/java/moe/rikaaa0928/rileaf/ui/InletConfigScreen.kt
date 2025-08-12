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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.InletConfig
import java.util.UUID

class InletConfigViewModel(private val configManager: ConfigManager) : ViewModel() {
    private var _inlets = mutableStateOf<List<InletConfig>>(emptyList())
    val inlets: State<List<InletConfig>> = _inlets

    init {
        loadConfig()
    }

    private fun loadConfig() {
        val config = configManager.getConfig()
        _inlets.value = config.inlets
    }

    fun addInlet(inlet: InletConfig) {
        val newInlets = _inlets.value + inlet
        _inlets.value = newInlets
        saveConfig()
    }

    fun updateInlet(inlet: InletConfig) {
        _inlets.value = _inlets.value.map { if (it.id == inlet.id) inlet else it }
        saveConfig()
    }

    fun deleteInlet(inletId: String) {
        _inlets.value = _inlets.value.filter { it.id != inletId }
        saveConfig()
    }

    private fun saveConfig() {
        val config = configManager.getConfig().copy(
            inlets = _inlets.value
        )
        configManager.saveConfig(config)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InletConfigScreen(
    configManager: ConfigManager,
    onNavigateBack: () -> Unit
) {
    val viewModel: InletConfigViewModel = viewModel { InletConfigViewModel(configManager) }
    val inlets by viewModel.inlets

    var showEditDialog by remember { mutableStateOf<InletConfig?>(null) }
    var showAddDialog by remember { mutableState of<String?>(null) }

    val hasHttpInlet = inlets.any { it.type == "http" }
    val hasSocksInlet = inlets.any { it.type == "socks" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("入口配置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDialog = "http" },
                    enabled = !hasHttpInlet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("添加 HTTP 入口")
                }
                Button(
                    onClick = { showAddDialog = "socks" },
                    enabled = !hasSocksInlet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("添加 SOCKS 入口")
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(inlets) { inlet ->
                    InletConfigItem(
                        inlet = inlet,
                        onEdit = { showEditDialog = inlet },
                        onDelete = { viewModel.deleteInlet(inlet.id) }
                    )
                }
            }
        }
    }

    showAddDialog?.let { type ->
        InletEditDialog(
            inlet = null,
            type = type,
            onDismiss = { showAddDialog = null },
            onSave = { inlet ->
                viewModel.addInlet(inlet)
                showAddDialog = null
            }
        )
    }

    showEditDialog?.let { inlet ->
        InletEditDialog(
            inlet = inlet,
            type = inlet.type,
            onDismiss = { showEditDialog = null },
            onSave = { updatedInlet ->
                viewModel.updateInlet(updatedInlet)
                showEditDialog = null
            }
        )
    }
}

@Composable
fun InletConfigItem(
    inlet: InletConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                        text = inlet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${inlet.type.uppercase()} - ${inlet.address}:${inlet.port}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
        }
    }
}

@Composable
fun InletEditDialog(
    inlet: InletConfig?,
    type: String,
    onDismiss: () -> Unit,
    onSave: (InletConfig) -> Unit
) {
    var name by remember { mutableStateOf(inlet?.name ?: "") }
    var address by remember { mutableStateOf(inlet?.address ?: "0.0.0.0") }
    var port by remember { mutableStateOf(inlet?.port?.toString() ?: if (type == "http") "8080" else "1080") }

    var isNameError by remember { mutableStateOf(false) }
    var isAddressError by remember { mutableStateOf(false) }
    var isPortError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        isNameError = name.isBlank()
        isAddressError = address.isBlank()
        isPortError = port.toIntOrNull() == null
        return !isNameError && !isAddressError && !isPortError
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inlet == null) "添加入口" else "编辑入口") },
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
                    label = { Text("入口名称") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isNameError,
                    supportingText = { if (isNameError) Text("名称不能为空") }
                )

                OutlinedTextField(
                    value = type.uppercase(),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("入口类型") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        isAddressError = false
                    },
                    label = { Text("绑定地址") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isAddressError,
                    supportingText = { if (isAddressError) Text("地址不能为空") }
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validate()) {
                        val newInlet = InletConfig(
                            id = inlet?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            type = type,
                            address = address,
                            port = port.toInt()
                        )
                        onSave(newInlet)
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
