package moe.rikaaa0928.rileaf

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.ui.*

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startVpnService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val configManager = ConfigManager(this@MainActivity)
            
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
                        onNavigateToAppFilter = { navController.navigate("app_filter") }
                    )
                }
                
                composable("proxy_config") {
                    val mainEntry = navController.getBackStackEntry("main")
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
