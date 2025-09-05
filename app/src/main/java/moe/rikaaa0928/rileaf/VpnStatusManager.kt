package moe.rikaaa0928.rileaf

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.net.VpnService
import android.util.Log
import android.app.ActivityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.rikaaa0928.rileaf.data.VpnStatus
import moe.rikaaa0928.rileaf.data.VpnStatusInfo
import moe.rikaaa0928.rileaf.data.VpnStatusListener

/**
 * Centralized VPN status manager that tracks the actual state of VPN service
 * and provides reliable status updates to UI components
 */
class VpnStatusManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "VpnStatusManager"
        
        // Broadcast actions for status updates
        const val ACTION_VPN_STATUS_CHANGED = "moe.rikaaa0928.rileaf.VPN_STATUS_CHANGED"
        const val EXTRA_STATUS_INFO = "status_info"
        
        @Volatile
        private var INSTANCE: VpnStatusManager? = null
        
        fun getInstance(context: Context): VpnStatusManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VpnStatusManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val _statusFlow = MutableStateFlow(
        VpnStatusInfo(VpnStatus.DISCONNECTED, "VPN is not connected")
    )
    val statusFlow: StateFlow<VpnStatusInfo> = _statusFlow.asStateFlow()
    
    private val listeners = mutableSetOf<VpnStatusListener>()
    private var statusReceiver: BroadcastReceiver? = null
    
    init {
        registerStatusReceiver()
        checkInitialStatus()
    }
    
    private fun registerStatusReceiver() {
        statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_VPN_STATUS_CHANGED) {
                    val statusJson = intent.getStringExtra(EXTRA_STATUS_INFO)
                    statusJson?.let { json ->
                        try {
                            // Parse status info from JSON and update
                            updateStatusFromService(json)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse status update", e)
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter(ACTION_VPN_STATUS_CHANGED)
        context.registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }
    
    private fun checkInitialStatus() {
        // Check if VPN service is currently running
        val vpnActive = isVpnServiceRunning()
        if (vpnActive) {
            updateStatus(VpnStatus.CONNECTED, "VPN service is running")
        } else {
            updateStatus(VpnStatus.DISCONNECTED, "VPN is not connected")
        }
    }
    
    private fun isVpnServiceRunning(): Boolean {
        // Check if our VPN service is currently active
        // This is a heuristic check - Android doesn't provide direct API
        return try {
            val activeInterface = VpnService.prepare(context)
            activeInterface == null && isLeafVpnServiceRunning()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isLeafVpnServiceRunning(): Boolean {
        // Check if our specific VPN service is in the running services
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Integer.MAX_VALUE)
        return services.any { service ->
            service.service.className == LeafVpnService::class.java.name
        }
    }
    
    fun updateStatus(status: VpnStatus, message: String = "", errorCode: String? = null) {
        val statusInfo = VpnStatusInfo(
            status = status,
            message = message,
            errorCode = errorCode,
            timestamp = System.currentTimeMillis(),
            rustProcessRunning = status == VpnStatus.CONNECTED,
            vpnInterfaceActive = status == VpnStatus.CONNECTED
        )
        
        updateStatusInfo(statusInfo)
    }
    
    fun updateStatusInfo(statusInfo: VpnStatusInfo) {
        Log.d(TAG, "Status updated: ${statusInfo.status} - ${statusInfo.message}")
        _statusFlow.value = statusInfo
        
        // Notify all listeners
        listeners.forEach { listener ->
            try {
                listener.onVpnStatusChanged(statusInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying status listener", e)
            }
        }
        
        // Send broadcast for other components
        val intent = Intent(ACTION_VPN_STATUS_CHANGED).apply {
            putExtra(EXTRA_STATUS_INFO, statusInfo.toString()) // You might want to use JSON serialization
        }
        context.sendBroadcast(intent)
    }
    
    private fun updateStatusFromService(statusJson: String) {
        // Parse and update status from service broadcast
        // Implementation depends on JSON serialization format
        Log.d(TAG, "Received status update from service: $statusJson")
    }
    
    fun addListener(listener: VpnStatusListener) {
        listeners.add(listener)
        // Immediately notify with current status
        listener.onVpnStatusChanged(_statusFlow.value)
    }
    
    fun removeListener(listener: VpnStatusListener) {
        listeners.remove(listener)
    }
    
    fun getCurrentStatus(): VpnStatusInfo = _statusFlow.value
    
    fun isConnected(): Boolean = _statusFlow.value.status == VpnStatus.CONNECTED
    
    fun isConnecting(): Boolean = _statusFlow.value.status == VpnStatus.CONNECTING
    
    fun hasError(): Boolean = _statusFlow.value.status.name.startsWith("ERROR_")
    
    fun getErrorMessage(): String? {
        return if (hasError()) _statusFlow.value.message else null
    }
    
    fun cleanup() {
        statusReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
        listeners.clear()
    }
}