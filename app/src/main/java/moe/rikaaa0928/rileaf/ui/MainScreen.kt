package moe.rikaaa0928.rileaf.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.rikaaa0928.rileaf.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.rikaaa0928.rileaf.data.ConfigManager

class MainViewModel(private val configManager: ConfigManager) : ViewModel() {
    private var _isVpnRunning = mutableStateOf(false)
    val isVpnRunning: State<Boolean> = _isVpnRunning
    
    private var _currentProxyName = mutableStateOf("")
    val currentProxyName: State<String> = _currentProxyName
    
    init {
        loadCurrentProxy()
    }
    
    private fun loadCurrentProxy() {
        val config = configManager.getConfig()
        val selectedProxy = config.proxies.find { it.id == config.selectedProxyId }
        _currentProxyName.value = selectedProxy?.name ?: ""
    }
    
    fun setVpnRunning(running: Boolean) {
        _isVpnRunning.value = running
    }
    
    fun refreshCurrentProxy() {
        loadCurrentProxy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    configManager: ConfigManager,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onNavigateToProxyConfig: () -> Unit,
    onNavigateToVpnConfig: () -> Unit,
    onNavigateToAppFilter: () -> Unit,
    onNavigateToInletConfig: () -> Unit,
    onNavigateToLanguage: () -> Unit
) {
    val viewModel: MainViewModel = viewModel { MainViewModel(configManager) }
    val isVpnRunning by viewModel.isVpnRunning
    val currentProxyName by viewModel.currentProxyName
    val unselectedProxyText = stringResource(R.string.unselected_proxy)

    // 当从配置页面返回时刷新代理信息
    LaunchedEffect(Unit) {
        viewModel.refreshCurrentProxy()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon),
                            contentDescription = stringResource(R.string.app_name_vpn),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToLanguage) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.language)
                        )
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
            // VPN 状态卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isVpnRunning)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isVpnRunning)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isVpnRunning) stringResource(R.string.vpn_connected) else stringResource(R.string.vpn_disconnected),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isVpnRunning) stringResource(R.string.your_connection_is_protected) else stringResource(R.string.tap_to_connect_to_protect),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isVpnRunning) {
                                onStopVpn()
                                viewModel.setVpnRunning(false)
                            } else {
                                onStartVpn()
                                viewModel.setVpnRunning(true)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isVpnRunning) stringResource(R.string.disconnect) else stringResource(R.string.connect_vpn))
                    }
                }
            }

            // 当前代理信息
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.current_proxy),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentProxyName.isNotEmpty()) currentProxyName else unselectedProxyText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = if (currentProxyName.isNotEmpty())
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (currentProxyName.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 配置选项
            Text(
                text = stringResource(R.string.configuration_management),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 代理配置
            ConfigOptionCard(
                title = stringResource(R.string.proxy_config),
                description = stringResource(R.string.manage_proxy_server_settings),
                icon = Icons.Default.Settings,
                onClick = onNavigateToProxyConfig
            )

            // VPN 配置
            ConfigOptionCard(
                title = stringResource(R.string.vpn_config),
                description = stringResource(R.string.network_interface_and_connection_settings),
                icon = Icons.Default.Build,
                onClick = onNavigateToVpnConfig
            )

            // 应用分流
            ConfigOptionCard(
                title = stringResource(R.string.app_bypass),
                description = stringResource(R.string.select_apps_to_use_vpn),
                icon = Icons.Default.Check,
                onClick = onNavigateToAppFilter
            )

            // 入口配置
            ConfigOptionCard(
                title = stringResource(R.string.inlet_config),
                description = stringResource(R.string.add_extra_http_or_socks_inlets),
                icon = Icons.Default.Add,
                onClick = onNavigateToInletConfig
            )
        }
    }
}

@Composable
fun ConfigOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}