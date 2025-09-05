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
import moe.rikaaa0928.rileaf.VpnStatusManager
import moe.rikaaa0928.rileaf.data.VpnStatus
import moe.rikaaa0928.rileaf.data.VpnStatusInfo
import androidx.compose.runtime.collectAsState

class MainViewModel(private val configManager: ConfigManager) : ViewModel() {
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
    
    fun refreshCurrentProxy() {
        loadCurrentProxy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    configManager: ConfigManager,
    statusManager: VpnStatusManager,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onNavigateToProxyConfig: () -> Unit,
    onNavigateToVpnConfig: () -> Unit,
    onNavigateToAppFilter: () -> Unit,
    onNavigateToInletConfig: () -> Unit,
    onNavigateToAppSettings: () -> Unit
) {
    val viewModel: MainViewModel = viewModel { MainViewModel(configManager) }
    val vpnStatusInfo by statusManager.statusFlow.collectAsState()
    val currentProxyName by viewModel.currentProxyName
    
    // 当从配置页面返回时刷新代理信息
    LaunchedEffect(Unit) {
        viewModel.refreshCurrentProxy()
    }
    
    val isVpnRunning = vpnStatusInfo.status == VpnStatus.CONNECTED
    val isVpnConnecting = vpnStatusInfo.status == VpnStatus.CONNECTING || vpnStatusInfo.status == VpnStatus.DISCONNECTING
    val hasVpnError = vpnStatusInfo.status.name.startsWith("ERROR_")
    
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
                            contentDescription = "Rileaf VPN",
                            modifier = Modifier.size(32.dp)
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
                    containerColor = when {
                        isVpnRunning -> MaterialTheme.colorScheme.primaryContainer
                        hasVpnError -> MaterialTheme.colorScheme.errorContainer
                        isVpnConnecting -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when {
                            isVpnRunning -> Icons.Default.Lock
                            hasVpnError -> Icons.Default.Warning
                            isVpnConnecting -> Icons.Default.Refresh
                            else -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = when {
                            isVpnRunning -> MaterialTheme.colorScheme.primary
                            hasVpnError -> MaterialTheme.colorScheme.error
                            isVpnConnecting -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = when (vpnStatusInfo.status) {
                            VpnStatus.CONNECTED -> stringResource(R.string.vpn_connected)
                            VpnStatus.CONNECTING -> "正在连接..."
                            VpnStatus.DISCONNECTING -> "正在断开..."
                            VpnStatus.ERROR_PERMISSION_DENIED -> "权限被拒绝"
                            VpnStatus.ERROR_ANOTHER_VPN_ACTIVE -> "其他VPN正在运行"
                            VpnStatus.ERROR_VPN_REVOKED -> "VPN权限被撤销"
                            VpnStatus.ERROR_ESTABLISH_FAILED -> "连接失败"
                            VpnStatus.ERROR_CONFIG_INVALID -> "配置错误"
                            VpnStatus.ERROR_RUST_STARTUP_FAILED -> "服务启动失败"
                            VpnStatus.ERROR_SYSTEM_KILLED -> "服务被系统终止"
                            else -> stringResource(R.string.vpn_disconnected)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = vpnStatusInfo.message.ifEmpty {
                            stringResource(if (isVpnRunning) R.string.connection_protected else R.string.click_to_connect)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasVpnError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (isVpnRunning) {
                                onStopVpn()
                            } else {
                                onStartVpn()
                            }
                        },
                        enabled = !isVpnConnecting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(
                                if (isVpnRunning) R.string.disconnect_vpn 
                                else if (isVpnConnecting) R.string.vpn_connecting 
                                else R.string.connect_vpn
                            )
                        )
                    }
                    
                    // 显示错误详情
                    if (hasVpnError && vpnStatusInfo.errorCode != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "错误代码: ${vpnStatusInfo.errorCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
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
                            text = if (currentProxyName.isNotEmpty()) currentProxyName else stringResource(R.string.no_proxy_selected),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = if (currentProxyName != "未选择代理") 
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
                text = stringResource(R.string.config_management),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 代理配置
            ConfigOptionCard(
                title = stringResource(R.string.proxy_config),
                description = stringResource(R.string.proxy_config_desc),
                icon = Icons.Default.Settings,
                onClick = onNavigateToProxyConfig
            )
            
            // VPN 配置
            ConfigOptionCard(
                title = stringResource(R.string.vpn_config),
                description = stringResource(R.string.vpn_config_desc),
                icon = Icons.Default.Build,
                onClick = onNavigateToVpnConfig
            )
            
            // 应用分流
            ConfigOptionCard(
                title = stringResource(R.string.app_filter),
                description = stringResource(R.string.app_filter_desc),
                icon = Icons.Default.Check,
                onClick = onNavigateToAppFilter
            )

            // 入口配置
            ConfigOptionCard(
                title = stringResource(R.string.inlet_config),
                description = stringResource(R.string.inlet_config_desc),
                icon = Icons.Default.Add,
                onClick = onNavigateToInletConfig
            )

            // 应用设置
            ConfigOptionCard(
                title = stringResource(R.string.app_settings),
                description = stringResource(R.string.app_settings_desc),
                icon = Icons.Default.Settings,
                onClick = onNavigateToAppSettings
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