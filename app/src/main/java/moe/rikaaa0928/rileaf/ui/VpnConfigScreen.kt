package moe.rikaaa0928.rileaf.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.VpnConfig

class VpnConfigViewModel(private val configManager: ConfigManager) : ViewModel() {
    private var _vpnConfig = mutableStateOf(VpnConfig())
    val vpnConfig: State<VpnConfig> = _vpnConfig
    
    init {
        loadConfig()
    }
    
    private fun loadConfig() {
        _vpnConfig.value = configManager.getConfig().vpnConfig
    }
    
    fun updateConfig(newConfig: VpnConfig) {
        _vpnConfig.value = newConfig
        val config = configManager.getConfig().copy(vpnConfig = newConfig)
        configManager.saveConfig(config)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnConfigScreen(
    configManager: ConfigManager,
    onNavigateBack: () -> Unit
) {
    val viewModel: VpnConfigViewModel = viewModel { VpnConfigViewModel(configManager) }
    val vpnConfig by viewModel.vpnConfig
    
    var vpnAddress by remember { mutableStateOf("") }
    var vpnNetmask by remember { mutableStateOf("") }
    var dnsServer by remember { mutableStateOf("") }
    var sessionName by remember { mutableStateOf("") }
    var logLevel by remember { mutableStateOf("") }
    var bypassLan by remember { mutableStateOf(true) }
    var routingDomainResolve by remember { mutableStateOf(true) }
    
    // 同步状态
    LaunchedEffect(vpnConfig) {
        vpnAddress = vpnConfig.vpnAddress
        vpnNetmask = vpnConfig.vpnNetmask.toString()
        dnsServer = vpnConfig.dnsServer
        sessionName = vpnConfig.sessionName
        logLevel = vpnConfig.logLevel
        bypassLan = vpnConfig.bypassLan
        routingDomainResolve = vpnConfig.routingDomainResolve
    }
    
    val logLevelOptions = listOf("error", "warn", "info", "debug", "trace")
    var expandedLogLevel by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VPN 配置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val newConfig = VpnConfig(
                                vpnAddress = vpnAddress,
                                vpnNetmask = vpnNetmask.toIntOrNull() ?: 24,
                                dnsServer = dnsServer,
                                sessionName = sessionName,
                                logLevel = logLevel,
                                bypassLan = bypassLan,
                                routingDomainResolve = routingDomainResolve
                            )
                            viewModel.updateConfig(newConfig)
                            onNavigateBack()
                        }
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "网络设置",
                style = MaterialTheme.typography.titleMedium
            )
            
            OutlinedTextField(
                value = vpnAddress,
                onValueChange = { vpnAddress = it },
                label = { Text("VPN IP 地址") },
                placeholder = { Text("10.9.28.2") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = vpnNetmask,
                onValueChange = { vpnNetmask = it },
                label = { Text("子网掩码 (CIDR)") },
                placeholder = { Text("24") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = dnsServer,
                onValueChange = { dnsServer = it },
                label = { Text("DNS 服务器") },
                placeholder = { Text("8.8.8.8") },
                modifier = Modifier.fillMaxWidth()
            )
            
            
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("会话名称") },
                placeholder = { Text("Rileaf VPN") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "日志设置",
                style = MaterialTheme.typography.titleMedium
            )
            
            Box {
                OutlinedTextField(
                    value = logLevel.uppercase(),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("日志级别") },
                    trailingIcon = { 
                        IconButton(onClick = { expandedLogLevel = !expandedLogLevel }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expandedLogLevel,
                    onDismissRequest = { expandedLogLevel = false }
                ) {
                    logLevelOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.uppercase()) },
                            onClick = {
                                logLevel = option
                                expandedLogLevel = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "路由设置",
                style = MaterialTheme.typography.titleMedium
            )
            
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
                                text = "局域网直连",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "绕过代理直接访问局域网地址",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bypassLan,
                            onCheckedChange = { bypassLan = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "域名解析路由",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "对域名进行解析后再进行路由匹配",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = routingDomainResolve,
                            onCheckedChange = { routingDomainResolve = it }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "说明",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• VPN IP 地址：虚拟网络接口的 IP 地址\n" +
                                "• 子网掩码：CIDR 格式的网络掩码，通常为 24\n" +
                                "• DNS 服务器：VPN 连接使用的 DNS 服务器\n" +
                                "• 会话名称：VPN 连接显示的名称\n" +
                                "• 日志级别：调试时可设置为 debug 或 info\n" +
                                "• 局域网直连：开启后局域网流量不通过代理\n" +
                                "• 域名解析路由：开启后对域名解析IP再匹配路由规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}