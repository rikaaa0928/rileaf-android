package moe.rikaaa0928.rileaf.data

import kotlinx.serialization.Serializable

@Serializable
data class ProxyConfig(
    val id: String,
    val name: String,
    val type: String,
    val server: String,
    val port: Int,
    val password: String? = null,
    val isEnabled: Boolean = true
)

@Serializable
data class VpnConfig(
    val vpnAddress: String = "10.9.28.2",
    val vpnNetmask: Int = 24,
    val dnsServer: String = "8.8.8.8",
    val sessionName: String = "Rileaf VPN",
    val logLevel: String = "error",
    val bypassLan: Boolean = true,
    val routingDomainResolve: Boolean = true
)

@Serializable
data class AppFilterConfig(
    val isWhitelistMode: Boolean = false, // false = blacklist, true = whitelist
    val selectedApps: Set<String> = emptySet()
)

@Serializable
data class ApplicationInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)

@Serializable
data class RileafConfig(
    val proxies: List<ProxyConfig> = emptyList(),
    val selectedProxyId: String? = null,
    val vpnConfig: VpnConfig = VpnConfig(),
    val appFilterConfig: AppFilterConfig = AppFilterConfig()
)