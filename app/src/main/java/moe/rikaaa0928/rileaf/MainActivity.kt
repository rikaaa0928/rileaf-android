package moe.rikaaa0928.rileaf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.LanguageManager
import moe.rikaaa0928.rileaf.data.VpnStatus
import moe.rikaaa0928.rileaf.debug.VpnStatusDebugHelper
import moe.rikaaa0928.rileaf.ui.*

class MainActivity : ComponentActivity() {

    private lateinit var statusManager: VpnStatusManager
    private lateinit var debugHelper: VpnStatusDebugHelper
    
    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    startVpnService()
                }
                Activity.RESULT_CANCELED -> {
                    statusManager.updateStatus(VpnStatus.ERROR_PERMISSION_DENIED, "VPN permission was denied by user")
                }
                else -> {
                    statusManager.updateStatus(VpnStatus.ERROR_PREPARE_FAILED, "VPN preparation failed with result code: ${result.resultCode}")
                }
            }
        }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val configManager = ConfigManager(newBase)
            val languageManager = LanguageManager(configManager)
            val updatedContext = languageManager.updateContextLocale(newBase)
            super.attachBaseContext(updatedContext)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusManager = VpnStatusManager.getInstance(this)
        debugHelper = VpnStatusDebugHelper.getInstance(this)
        
        // Start debug monitoring - always enabled for better debugging
        debugHelper.startMonitoring()
        
        setContent {
            val navController = rememberNavController()
            val configManager = ConfigManager(this@MainActivity)
            var refreshKey by remember { mutableIntStateOf(0) }
            
            key(refreshKey) {
                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        MainScreen(
                            configManager = configManager,
                            statusManager = statusManager,
                            onStartVpn = { 
                                try {
                                    val intent = VpnService.prepare(this@MainActivity)
                                    if (intent != null) {
                                        statusManager.updateStatus(VpnStatus.CONNECTING, "Requesting VPN permission...")
                                        vpnPermissionLauncher.launch(intent)
                                    } else {
                                        startVpnService()
                                    }
                                } catch (e: Exception) {
                                    statusManager.updateStatus(VpnStatus.ERROR_PREPARE_FAILED, "Failed to prepare VPN: ${e.message}")
                                }
                            },
                            onStopVpn = { stopVpnService() },
                            onNavigateToProxyConfig = { navController.navigate("proxy_config") },
                            onNavigateToVpnConfig = { navController.navigate("vpn_config") },
                            onNavigateToAppFilter = { navController.navigate("app_filter") },
                            onNavigateToInletConfig = { navController.navigate("inlet_config") },
                            onNavigateToAppSettings = { navController.navigate("app_settings") }
                        )
                    }
                    
                    composable("proxy_config") {
                        ProxyConfigScreen(
                            configManager = configManager,
                            isVpnConnected = statusManager.isConnected(),
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("vpn_config") {
                        VpnConfigScreen(
                            configManager = configManager,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("app_filter") {
                        AppFilterScreen(
                            configManager = configManager,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("inlet_config") {
                        InletConfigScreen(
                            configManager = configManager,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("app_settings") {
                        AppSettingsScreen(
                            configManager = configManager,
                            onNavigateBack = { navController.popBackStack() },
                            onLanguageChanged = { 
                                refreshKey++
                                recreate()
                            }
                        )
                    }

                    composable("language_settings") {
                        LanguageSettingsScreen(
                            configManager = configManager,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        debugHelper.stopMonitoring()
        super.onDestroy()
    }

    private fun startVpnService() {
        try {
            val intent = Intent(this, LeafVpnService::class.java)
            intent.action = LeafVpnService.ACTION_CONNECT
            startService(intent)
        } catch (e: Exception) {
            statusManager.updateStatus(VpnStatus.ERROR_PREPARE_FAILED, "Failed to start VPN service: ${e.message}")
        }
    }

    private fun stopVpnService() {
        try {
            val intent = Intent(this, LeafVpnService::class.java)
            intent.action = LeafVpnService.ACTION_DISCONNECT
            startService(intent)
        } catch (e: Exception) {
            statusManager.updateStatus(VpnStatus.ERROR_RUST_RUNTIME_ERROR, "Failed to stop VPN service: ${e.message}")
        }
    }
}
