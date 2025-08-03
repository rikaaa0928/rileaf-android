package moe.rikaaa0928.rileaf

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uniffi.leafuniffi.*

class LeafVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var leafThread: Thread? = null
    private val rtId: UShort = 1u

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createVpnInterface()
        val socksAddress = intent?.getStringExtra("socks_address") ?: "127.0.0.1:1080"
        leafThread = Thread {
            runLeaf(socksAddress)
        }
        leafThread?.start()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
//            leafReload(rtId, "")
            leafShutdown(rtId)
            vpnInterface?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        leafThread?.interrupt()
    }

    private fun createVpnInterface() {
        val builder = Builder()
        builder.addAddress("10.9.28.2", 24)
        builder.addRoute("0.0.0.0", 0)
//        builder.addDnsServer("8.8.8.8")
        builder.setSession(getString(R.string.app_name))
//        builder.addAllowedApplication("moe.rikaaa0928.nettest")
        builder.addDisallowedApplication(packageName)
//        builder.addDisallowedApplication("uniffi.leafuniffi")
        vpnInterface = builder.establish()
    }

    private fun runLeaf(socksAddress: String) {
        val fd = vpnInterface?.detachFd()?: return
//        val socksIpPort = socksAddress.replace(":", ", ")
        val configContent = """
            [General]
            loglevel = error
            logoutput = console
            dns-server = 8.8.8.8
            tun-fd = ${fd.toLong()}
            [Proxy]
            ROG = rog, 127.0.0.1, 443, password=123
            [Host]
            """.trimIndent()
        try {
            leafRunWithConfigString(rtId, configContent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
