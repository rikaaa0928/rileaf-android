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
        _currentProxyName.value = selectedProxy?.name ?: "未选择代理"
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
    onNavigateToAppFilter: () -> Unit
) {
    val viewModel: MainViewModel = viewModel { MainViewModel(configManager) }
    val isVpnRunning by viewModel.isVpnRunning
    val currentProxyName by viewModel.currentProxyName
    
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
                        text = if (isVpnRunning) "VPN 已连接" else "VPN 未连接",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = if (isVpnRunning) "您的连接已受保护" else "点击连接开始保护",
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
                        Text(if (isVpnRunning) "断开连接" else "连接 VPN")
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
                        text = "当前代理",
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
                            text = currentProxyName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = if (currentProxyName != "未选择代理") 
                                Icons.Default.CheckCircle 
                            else 
                                Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (currentProxyName != "未选择代理") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 配置选项
            Text(
                text = "配置管理",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 代理配置
            ConfigOptionCard(
                title = "代理配置",
                description = "管理代理服务器设置",
                icon = Icons.Default.Settings,
                onClick = onNavigateToProxyConfig
            )
            
            // VPN 配置
            ConfigOptionCard(
                title = "VPN 配置",
                description = "网络接口和连接设置",
                icon = Icons.Default.Build,
                onClick = onNavigateToVpnConfig
            )
            
            // 应用分流
            ConfigOptionCard(
                title = "应用分流",
                description = "选择哪些应用使用 VPN",
                icon = Icons.Default.Check,
                onClick = onNavigateToAppFilter
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