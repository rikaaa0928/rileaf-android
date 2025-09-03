package moe.rikaaa0928.rileaf

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uniffi.leafuniffi.*
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.VpnState
import moe.rikaaa0928.rileaf.data.VpnStateRepository

class LeafVpnService : VpnService() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        const val ACTION_CONNECT = "moe.rikaaa0928.rileaf.CONNECT"
        const val ACTION_DISCONNECT = "moe.rikaaa0928.rileaf.DISCONNECT"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "vpn_service_channel"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var leafThread: Thread? = null
    private val rtId: UShort = 1u
    private lateinit var configManager: ConfigManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        configManager = ConfigManager(this)
        createNotificationChannel()

        if (VpnService.prepare(this) != null) {
            // VPN not prepared, probably due to always-on without permission
            stopVpnService()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_DISCONNECT) {
            stopVpnService()
            return START_NOT_STICKY
        }

        VpnStateRepository.updateState(VpnState.CONNECTING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createNotification(false), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, createNotification(false))
        }
        startVpnService(intent)
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpnService()
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

        try {
            createVpnInterface()
        } catch (e: Exception) {
            e.printStackTrace()
            stopVpnService()
            return
        }

        val notification = createNotification(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        leafThread = Thread {
            runLeaf()
        }
        leafThread?.start()
        VpnStateRepository.updateState(VpnState.CONNECTED)
    }

    private fun stopVpnService() {
        VpnStateRepository.updateState(VpnState.DISCONNECTING)
        if (vpnInterface == null) {
            VpnStateRepository.updateState(VpnState.DISCONNECTED)
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

        stopForeground(true)
        stopSelf()
        VpnStateRepository.updateState(VpnState.DISCONNECTED)
    }

    private fun createVpnInterface() {
        val config = configManager.getConfig()
        val vpnConfig = config.vpnConfig
        val appFilter = config.appFilterConfig

        val builder = Builder()
        builder.addAddress(vpnConfig.vpnAddress, vpnConfig.vpnNetmask)
        builder.addRoute("0.0.0.0", 0)

        builder.addDnsServer(vpnConfig.dnsServer)
        builder.setSession(vpnConfig.sessionName)

        try {
            if (appFilter.isWhitelistMode) {
                for (appPackageName in appFilter.selectedApps) {
                    if (appPackageName != this.packageName) {
                        builder.addAllowedApplication(appPackageName)
                    }
                }
            } else {
                builder.addDisallowedApplication(packageName)
                for (appPackageName in appFilter.selectedApps) {
                    if (appPackageName != this.packageName) {
                        builder.addDisallowedApplication(appPackageName)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        vpnInterface = builder.establish()
    }

    private fun runLeaf() {
        val fd = vpnInterface?.detachFd() ?: return
        val configContent = configManager.generateLeafConfigString(fd.toLong())
        Log.i("leaf start", configContent)
        try {
            leafRunWithConfigString(rtId, configContent)
        } catch (e: Exception) {
            e.printStackTrace()
            stopVpnService()
        }
    }
    
    private fun createNotificationChannel() {
        val name = "VPN Service"
        val descriptionText = "VPN service is running"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(isConnected: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = if (isConnected) "VPN Connected" else "VPN Connecting..."
        val content = if (isConnected) "VPN is active and protecting your connection" else "Establishing VPN connection"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
