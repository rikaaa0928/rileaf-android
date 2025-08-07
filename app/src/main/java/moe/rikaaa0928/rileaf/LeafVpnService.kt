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
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uniffi.leafuniffi.*
import moe.rikaaa0928.rileaf.data.ConfigManager

class LeafVpnService : VpnService() {

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
        
        if (intent != null && ACTION_DISCONNECT.equals(intent.action)) {
            stopVpnService()
            return START_NOT_STICKY
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createNotification(false), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, createNotification(false))
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
        
        // Update notification to show connected status
        val notification = createNotification(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        leafThread = Thread {
            runLeaf()
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

        stopForeground(true)
        stopSelf()
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
        
        // 根据配置应用过滤规则
        try {
            if (appFilter.isWhitelistMode) {
                // 白名单模式：使用addAllowedApplication，只有选中的应用通过VPN
                
                // 只允许白名单中的应用通过VPN
                for (appPackageName in appFilter.selectedApps) {
                    if (appPackageName != this.packageName) { // 跳过自己
                        builder.addAllowedApplication(appPackageName)
                    }
                }
            } else {
                // 黑名单模式：使用addDisallowedApplication，选中的应用不通过VPN
                // 始终排除自己
                builder.addDisallowedApplication(packageName)
                
                // 排除黑名单中的应用
                for (appPackageName in appFilter.selectedApps) {
                    if (appPackageName != this.packageName) { // 跳过自己（已经排除）
                        builder.addDisallowedApplication(appPackageName)
                    }
                }
                // 不在黑名单中的应用默认会通过VPN
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        vpnInterface = builder.establish()
    }

    private fun runLeaf() {
        val fd = vpnInterface?.detachFd()?: return
        val configContent = configManager.generateLeafConfigString(fd.toLong())
        
        try {
            leafRunWithConfigString(rtId, configContent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
