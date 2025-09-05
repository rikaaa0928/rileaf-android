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
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uniffi.leafuniffi.*
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.VpnStatus
import moe.rikaaa0928.rileaf.data.VpnStatusInfo

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
    private lateinit var statusManager: VpnStatusManager
    private var isShuttingDown = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        configManager = ConfigManager(this)
        statusManager = VpnStatusManager.getInstance(this)
        createNotificationChannel()
        
        if (intent != null && ACTION_DISCONNECT.equals(intent.action)) {
            stopVpnService()
            return START_NOT_STICKY
        }
        
        // Update status to connecting
        statusManager.updateStatus(VpnStatus.CONNECTING, "Starting VPN service...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createNotification(false), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, createNotification(false))
        }
        startVpnService(intent)
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("LeafVpnService", "Service destroyed")
        if (!isShuttingDown) {
            // Service was killed by system - report error
            statusManager.updateStatus(VpnStatus.ERROR_SYSTEM_KILLED, "VPN service was terminated by system")
        }
        super.onDestroy()
    }

    override fun onRevoke() {
        Log.d("LeafVpnService", "VPN permission revoked")
        statusManager.updateStatus(VpnStatus.ERROR_VPN_REVOKED, "VPN permission was revoked")
        stopVpnService()
        super.onRevoke()
    }

    private fun startVpnService(intent: Intent?) {
        if (vpnInterface != null) {
            statusManager.updateStatus(VpnStatus.CONNECTED, "VPN is already connected")
            return
        }

        try {
            createVpnInterface()
            
            if (vpnInterface == null) {
                statusManager.updateStatus(VpnStatus.ERROR_ESTABLISH_FAILED, "Failed to establish VPN interface")
                stopSelf()
                return
            }
            
            // Update notification to show connected status
            val notification = createNotification(true)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            // Start Rust service
            leafThread = Thread {
                runLeaf()
            }
            leafThread?.start()
            
        } catch (e: SecurityException) {
            Log.e("LeafVpnService", "Security exception - VPN permission denied or another VPN active", e)
            statusManager.updateStatus(VpnStatus.ERROR_ANOTHER_VPN_ACTIVE, "Another VPN is active or permission denied: ${e.message}")
            stopSelf()
        } catch (e: Exception) {
            Log.e("LeafVpnService", "Failed to start VPN service", e)
            statusManager.updateStatus(VpnStatus.ERROR_ESTABLISH_FAILED, "Failed to start VPN: ${e.message}")
            stopSelf()
        }
    }

    private fun stopVpnService() {
        if (vpnInterface == null) {
            statusManager.updateStatus(VpnStatus.DISCONNECTED, "VPN is already disconnected")
            return
        }

        isShuttingDown = true
        statusManager.updateStatus(VpnStatus.DISCONNECTING, "Stopping VPN service...")

        try {
            leafShutdown(rtId)
            Log.d("LeafVpnService", "Rust service shutdown successfully")
        } catch (e: Exception) {
            Log.e("LeafVpnService", "Error shutting down Rust service", e)
        }

        try {
            vpnInterface?.close()
            Log.d("LeafVpnService", "VPN interface closed")
        } catch (e: Exception) {
            Log.e("LeafVpnService", "Error closing VPN interface", e)
        }
        vpnInterface = null

        leafThread?.interrupt()
        leafThread = null

        statusManager.updateStatus(VpnStatus.DISCONNECTED, "VPN disconnected")
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
        
        try {
            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                throw SecurityException("VPN interface establishment returned null - another VPN might be active")
            }
            Log.d("LeafVpnService", "VPN interface established successfully")
        } catch (e: SecurityException) {
            Log.e("LeafVpnService", "Security exception establishing VPN interface", e)
            throw e // Re-throw to be handled by caller
        } catch (e: Exception) {
            Log.e("LeafVpnService", "Failed to establish VPN interface", e)
            throw e
        }
    }

    private fun runLeaf() {
        val fd = vpnInterface?.detachFd()
        if (fd == null) {
            Log.e("LeafVpnService", "VPN interface file descriptor is null")
            statusManager.updateStatus(VpnStatus.ERROR_ESTABLISH_FAILED, "VPN interface not available")
            return
        }
        
        val configContent = configManager.generateLeafConfigString(fd.toLong())
        Log.i("LeafVpnService", "Starting Rust service with config: $configContent")
        
        // First test the configuration
        try {
            val testResult = leafTestConfig("/dev/null") // Test with dummy path since we're using string config
            Log.d("LeafVpnService", "Config test result: $testResult")
        } catch (e: Exception) {
            Log.w("LeafVpnService", "Config test failed, but continuing with string config", e)
        }
        
        try {
            // Update status to connected before starting Rust service
            // since leafRunWithConfigString is a blocking call that runs until VPN stops
            statusManager.updateStatus(VpnStatus.CONNECTED, "VPN connected and running")
            Log.i("LeafVpnService", "Rust service starting...")
            
            // This call blocks until the VPN is shut down
            val result = leafRunWithConfigString(rtId, configContent)
            
            // This will only be reached when VPN is disconnected
            Log.i("LeafVpnService", "Rust service stopped with result: $result")
            
            // Check if this was an expected shutdown or an error
            if (!isShuttingDown) {
                when (result) {
                    ErrEnum.ERR_OK -> {
                        Log.i("LeafVpnService", "Rust service stopped normally")
                        statusManager.updateStatus(VpnStatus.DISCONNECTED, "VPN disconnected")
                    }
                    ErrEnum.ERR_CONFIG -> {
                        Log.e("LeafVpnService", "Rust service stopped due to config error")
                        statusManager.updateStatus(VpnStatus.ERROR_CONFIG_INVALID, "Invalid Leaf configuration")
                    }
                    else -> {
                        Log.e("LeafVpnService", "Rust service stopped unexpectedly: $result")
                        statusManager.updateStatus(VpnStatus.ERROR_RUST_RUNTIME_ERROR, "Rust service error: $result")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LeafVpnService", "Exception in Rust service", e)
            if (!isShuttingDown) {
                statusManager.updateStatus(VpnStatus.ERROR_RUST_STARTUP_FAILED, "Rust service exception: ${e.message}")
            }
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
