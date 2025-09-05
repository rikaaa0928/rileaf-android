package moe.rikaaa0928.rileaf.debug

import android.content.Context
import android.util.Log
import moe.rikaaa0928.rileaf.VpnStatusManager
import moe.rikaaa0928.rileaf.data.VpnStatus
import moe.rikaaa0928.rileaf.data.VpnStatusListener
import kotlinx.coroutines.*
import uniffi.leafuniffi.leafShutdown

/**
 * Debug helper to monitor VPN and Rust service status
 * This class helps ensure status accuracy and can detect when services get out of sync
 */
class VpnStatusDebugHelper private constructor(private val context: Context) : VpnStatusListener {
    
    companion object {
        private const val TAG = "VpnStatusDebugHelper"
        private const val MONITOR_INTERVAL_MS = 5000L // Check every 5 seconds
        
        @Volatile
        private var INSTANCE: VpnStatusDebugHelper? = null
        
        fun getInstance(context: Context): VpnStatusDebugHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VpnStatusDebugHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var monitorJob: Job? = null
    private val statusManager = VpnStatusManager.getInstance(context)
    
    fun startMonitoring() {
        stopMonitoring()
        
        statusManager.addListener(this)
        
        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    checkStatusConsistency()
                    delay(MONITOR_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in status monitoring", e)
                }
            }
        }
        
        Log.d(TAG, "Status monitoring started")
    }
    
    fun stopMonitoring() {
        monitorJob?.cancel()
        statusManager.removeListener(this)
        Log.d(TAG, "Status monitoring stopped")
    }
    
    private suspend fun checkStatusConsistency() {
        withContext(Dispatchers.Main) {
            val currentStatus = statusManager.getCurrentStatus()
            
            // Check if VPN service is actually running
            val serviceRunning = isVpnServiceActuallyRunning()
            
            // Check if Rust process responds
            val rustResponding = checkRustProcessHealth()
            
            Log.d(TAG, """
                Status Check:
                - Reported Status: ${currentStatus.status}
                - Service Running: $serviceRunning
                - Rust Responding: $rustResponding
                - VPN Interface Active: ${currentStatus.vpnInterfaceActive}
                - Rust Process Running: ${currentStatus.rustProcessRunning}
            """.trimIndent())
            
            // Detect inconsistencies and auto-correct if needed
            detectAndCorrectInconsistencies(currentStatus.status, serviceRunning, rustResponding)
        }
    }
    
    private fun isVpnServiceActuallyRunning(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val services = activityManager.getRunningServices(Integer.MAX_VALUE)
        return services.any { service ->
            service.service.className.contains("LeafVpnService")
        }
    }
    
    private fun checkRustProcessHealth(): Boolean {
        return try {
            // Try to shutdown a non-existent runtime ID to check if Rust is responsive
            // This is a "ping" to see if the Rust side is working
            val testResult = leafShutdown(999u) // Use a random ID that shouldn't exist
            // If we get a response (true or false), Rust is running
            true
        } catch (e: Exception) {
            // If we get an exception, Rust might not be running or there's an issue
            false
        }
    }
    
    private fun detectAndCorrectInconsistencies(
        reportedStatus: VpnStatus,
        serviceRunning: Boolean,
        rustResponding: Boolean
    ) {
        when {
            // Service claims to be connected but service is not running
            reportedStatus == VpnStatus.CONNECTED && !serviceRunning -> {
                Log.w(TAG, "Status inconsistency: Reported CONNECTED but service not running")
                statusManager.updateStatus(VpnStatus.ERROR_SYSTEM_KILLED, "VPN service was terminated unexpectedly")
            }
            
            // Service claims to be connected but Rust is not responding
            reportedStatus == VpnStatus.CONNECTED && serviceRunning && !rustResponding -> {
                Log.w(TAG, "Status inconsistency: Service running but Rust not responding")
                statusManager.updateStatus(VpnStatus.ERROR_RUST_RUNTIME_ERROR, "Rust service stopped responding")
            }
            
            // Service is running but status shows disconnected
            reportedStatus == VpnStatus.DISCONNECTED && serviceRunning -> {
                Log.w(TAG, "Status inconsistency: Service running but status shows disconnected")
                // Don't auto-correct this one as it might be during startup
            }
            
            // Service stuck in connecting state
            reportedStatus == VpnStatus.CONNECTING -> {
                val statusAge = System.currentTimeMillis() - statusManager.getCurrentStatus().timestamp
                if (statusAge > 30000) { // More than 30 seconds
                    Log.w(TAG, "Status stuck in CONNECTING for ${statusAge}ms")
                    if (!serviceRunning) {
                        statusManager.updateStatus(VpnStatus.ERROR_ESTABLISH_FAILED, "VPN connection timeout - service failed to start")
                    }
                }
            }
        }
    }
    
    override fun onVpnStatusChanged(statusInfo: moe.rikaaa0928.rileaf.data.VpnStatusInfo) {
        Log.d(TAG, "Status changed: ${statusInfo.status} - ${statusInfo.message}")
        
        // Additional logging for debugging
        when (statusInfo.status) {
            VpnStatus.ERROR_ANOTHER_VPN_ACTIVE -> {
                Log.w(TAG, "Another VPN is blocking our connection. Check for Always-On VPN or other VPN apps.")
            }
            VpnStatus.ERROR_VPN_REVOKED -> {
                Log.w(TAG, "VPN permission was revoked. User may have disabled it or another VPN took over.")
            }
            VpnStatus.ERROR_SYSTEM_KILLED -> {
                Log.w(TAG, "VPN service was killed by Android system. This may happen due to memory pressure.")
            }
            else -> {}
        }
    }
    
    fun logCurrentState() {
        val status = statusManager.getCurrentStatus()
        val serviceRunning = isVpnServiceActuallyRunning()
        val rustResponding = checkRustProcessHealth()
        
        Log.i(TAG, """
            === VPN Status Debug Info ===
            Current Status: ${status.status}
            Status Message: ${status.message}
            Error Code: ${status.errorCode}
            Timestamp: ${status.timestamp}
            VPN Interface Active: ${status.vpnInterfaceActive}
            Rust Process Running: ${status.rustProcessRunning}
            Service Actually Running: $serviceRunning
            Rust Actually Responding: $rustResponding
            Status Age: ${System.currentTimeMillis() - status.timestamp}ms
            ===========================
        """.trimIndent())
    }
}