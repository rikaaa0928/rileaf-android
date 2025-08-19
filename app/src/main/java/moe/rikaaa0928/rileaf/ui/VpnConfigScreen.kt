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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.rikaaa0928.rileaf.R
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
                title = { Text(stringResource(R.string.vpn_config)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                        Text(stringResource(R.string.save))
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
                text = stringResource(R.string.network_settings),
                style = MaterialTheme.typography.titleMedium
            )
            
            OutlinedTextField(
                value = vpnAddress,
                onValueChange = { vpnAddress = it },
                label = { Text(stringResource(R.string.vpn_ip_address)) },
                placeholder = { Text(stringResource(R.string.vpn_ip_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = vpnNetmask,
                onValueChange = { vpnNetmask = it },
                label = { Text(stringResource(R.string.subnet_mask)) },
                placeholder = { Text(stringResource(R.string.subnet_mask_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = dnsServer,
                onValueChange = { dnsServer = it },
                label = { Text(stringResource(R.string.dns_server)) },
                placeholder = { Text(stringResource(R.string.dns_server_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text(stringResource(R.string.session_name)) },
                placeholder = { Text(stringResource(R.string.session_name_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.log_settings),
                style = MaterialTheme.typography.titleMedium
            )
            
            Box {
                OutlinedTextField(
                    value = logLevel.uppercase(),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.log_level)) },
                    trailingIcon = { 
                        IconButton(onClick = { expandedLogLevel = !expandedLogLevel }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.select))
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
                text = stringResource(R.string.routing_settings),
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
                                text = stringResource(R.string.bypass_lan),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.bypass_lan_desc),
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
                                text = stringResource(R.string.routing_domain_resolve),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.routing_domain_resolve_desc),
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
                        text = stringResource(R.string.explanation),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.vpn_config_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}