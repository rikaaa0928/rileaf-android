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
import moe.rikaaa0928.rileaf.ui.*

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startVpnService()
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
                            onStartVpn = { 
                                val intent = VpnService.prepare(this@MainActivity)
                                if (intent != null) {
                                    vpnPermissionLauncher.launch(intent)
                                } else {
                                    startVpnService()
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
                        val mainEntry = remember { navController.getBackStackEntry("main") }
                        val mainViewModel: MainViewModel = viewModel(mainEntry) { MainViewModel(configManager) }
                        ProxyConfigScreen(
                            configManager = configManager,
                            isVpnConnected = mainViewModel.isVpnRunning.value,
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

    private fun startVpnService() {
        val intent = Intent(this, LeafVpnService::class.java)
        intent.action = "moe.rikaaa0928.rileaf.CONNECT"
        startService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, LeafVpnService::class.java)
        intent.action = "moe.rikaaa0928.rileaf.DISCONNECT"
        startService(intent)
    }
}
