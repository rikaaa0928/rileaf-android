package moe.rikaaa0928.rileaf.data

import kotlinx.serialization.Serializable

/**
 * Comprehensive VPN status enum that represents all possible states
 */
enum class VpnStatus {
    DISCONNECTED,           // VPN is not running
    CONNECTING,             // VPN permission granted, service starting
    CONNECTED,              // VPN interface established, Rust service running
    DISCONNECTING,          // VPN shutdown in progress
    ERROR_PERMISSION_DENIED, // VPN permission was denied
    ERROR_PREPARE_FAILED,   // VPN.prepare() failed
    ERROR_ESTABLISH_FAILED, // VPN interface creation failed
    ERROR_CONFIG_INVALID,   // Leaf configuration is invalid
    ERROR_RUST_STARTUP_FAILED, // Rust service failed to start
    ERROR_RUST_RUNTIME_ERROR,  // Rust service encountered runtime error
    ERROR_VPN_REVOKED,      // VPN permission was revoked by user/system
    ERROR_ANOTHER_VPN_ACTIVE, // Another VPN is running and blocking ours
    ERROR_ALWAYS_ON_VPN,    // Always-on VPN is enabled, blocking manual VPN
    ERROR_SYSTEM_KILLED     // VPN service was killed by Android system
}

/**
 * Detailed status information including error details
 */
@Serializable
data class VpnStatusInfo(
    val status: VpnStatus,
    val message: String = "",
    val errorCode: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val rustProcessRunning: Boolean = false,
    val vpnInterfaceActive: Boolean = false
)

/**
 * Interface for VPN status listeners
 */
interface VpnStatusListener {
    fun onVpnStatusChanged(statusInfo: VpnStatusInfo)
}