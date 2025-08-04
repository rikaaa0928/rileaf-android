package moe.rikaaa0928.rileaf

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            var socksAddress by remember { mutableStateOf("127.0.0.1:1080") }
            var isVpnRunning by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = socksAddress,
                    onValueChange = { socksAddress = it },
                    label = { Text("SOCKS5 Address") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (isVpnRunning) {
                        stopVpnService()
                        isVpnRunning = false
                    } else {
                        val intent = VpnService.prepare(this@MainActivity)
                        if (intent != null) {
                            vpnPermissionLauncher.launch(intent)
                        } else {
                            startVpnService(socksAddress)
                            isVpnRunning = true
                        }
                    }
                }) {
                    Text(if (isVpnRunning) "Stop VPN" else "Start VPN")
                }
            }
        }
    }

    private fun startVpnService(socksAddress: String = "127.0.0.1:1080") {
        val intent = Intent(this, LeafVpnService::class.java)
        intent.action = "moe.rikaaa0928.rileaf.CONNECT"
        intent.putExtra("socks_address", socksAddress)
        startService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, LeafVpnService::class.java)
        intent.action = "moe.rikaaa0928.rileaf.DISCONNECT"
        startService(intent)
    }
}
