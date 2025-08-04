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

    companion object {
        const val ACTION_CONNECT = "moe.rikaaa0928.rileaf.CONNECT"
        const val ACTION_DISCONNECT = "moe.rikaaa0928.rileaf.DISCONNECT"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var leafThread: Thread? = null
    private val rtId: UShort = 1u

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_DISCONNECT.equals(intent.action)) {
            stopVpnService()
            return START_NOT_STICKY
        }
        startVpnService(intent)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpnService()
        super.onRevoke()
    }

    private fun startVpnService(intent: Intent?) {
        if (vpnInterface != null) {
            return
        }

        createVpnInterface()
        val socksAddress = intent?.getStringExtra("socks_address") ?: "127.0.0.1:1080"
        leafThread = Thread {
            runLeaf(socksAddress)
        }
        leafThread?.start()
    }

    private fun stopVpnService() {
        if (vpnInterface == null) {
            return
        }

        try {
            leafShutdown(rtId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vpnInterface = null

        leafThread?.interrupt()
        leafThread = null

        stopSelf()
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
