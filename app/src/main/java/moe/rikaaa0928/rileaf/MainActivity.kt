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
import androidx.lifecycle.ViewModelProvider
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.LanguageManager
import moe.rikaaa0928.rileaf.data.VpnState
import moe.rikaaa0928.rileaf.ui.*

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startVpnService()
            }
        }

    private lateinit var mainViewModel: MainViewModel

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
        val configManager = ConfigManager(this)
        mainViewModel = ViewModelProvider(this, MainViewModelFactory(configManager))[MainViewModel::class.java]

        setContent {
            val navController = rememberNavController()
            var refreshKey by remember { mutableIntStateOf(0) }
            val vpnState by mainViewModel.vpnState.collectAsState()
            val currentProxyName by mainViewModel.currentProxyName

            key(refreshKey) {
                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        LaunchedEffect(Unit) {
                            mainViewModel.refreshCurrentProxy()
                        }
                        MainScreen(
                            vpnState = vpnState,
                            currentProxyName = currentProxyName,
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
                        ProxyConfigScreen(
                            configManager = configManager,
                            isVpnConnected = vpnState == VpnState.CONNECTED,
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
        intent.action = LeafVpnService.ACTION_CONNECT
        startService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, LeafVpnService::class.java)
        intent.action = LeafVpnService.ACTION_DISCONNECT
        startService(intent)
    }
}
