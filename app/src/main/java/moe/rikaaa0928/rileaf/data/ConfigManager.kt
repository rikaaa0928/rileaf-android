package moe.rikaaa0928.rileaf.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class ConfigManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rileaf_config", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    companion object {
        private const val KEY_CONFIG = "config"
        private const val DEFAULT_PROXY_ID = "default_rog"
    }
    
    fun getConfig(): RileafConfig {
        val configString = prefs.getString(KEY_CONFIG, null)
        return if (configString != null) {
            try {
                json.decodeFromString<RileafConfig>(configString)
            } catch (e: Exception) {
                getDefaultConfig()
            }
        } else {
            getDefaultConfig()
        }
    }
    
    fun saveConfig(config: RileafConfig) {
        val configString = json.encodeToString(config)
        prefs.edit().putString(KEY_CONFIG, configString).apply()
    }
    
    private fun getDefaultConfig(): RileafConfig {
        val defaultProxy = ProxyConfig(
            id = DEFAULT_PROXY_ID,
            name = "ROG 代理",
            type = "rog",
            server = "127.0.0.1",
            port = 443,
            password = "111"
        )
        
        return RileafConfig(
            proxies = listOf(defaultProxy),
            selectedProxyId = DEFAULT_PROXY_ID
        )
    }
    
    suspend fun getInstalledApps(): List<moe.rikaaa0928.rileaf.data.ApplicationInfo> {
        return withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            
            packages.mapNotNull { packageInfo ->
                val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                val appName = try {
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageInfo.packageName
                }
                
                moe.rikaaa0928.rileaf.data.ApplicationInfo(
                    packageName = packageInfo.packageName,
                    appName = appName,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }.sortedBy { it.appName }
        }
    }
    
    fun generateLeafConfigString(tunFd: Long): String {
        val config = getConfig()
        val selectedProxy = config.proxies.find { it.id == config.selectedProxyId }
            ?: config.proxies.firstOrNull()
            ?: return ""
        
        val proxyLine = when (selectedProxy.type.lowercase()) {
            "rog" -> "${selectedProxy.name} = rog, ${selectedProxy.server}, ${selectedProxy.port}, password=${selectedProxy.password}"
            "socks5" -> "${selectedProxy.name} = socks, ${selectedProxy.server}, ${selectedProxy.port}"
            else -> "${selectedProxy.name} = ${selectedProxy.type}, ${selectedProxy.server}, ${selectedProxy.port}"
        }
        
        val routingDomainResolveStr = if (config.vpnConfig.routingDomainResolve) "true" else "false"
        
        val lanRules = if (config.vpnConfig.bypassLan) {
            """
            IP-CIDR, 192.168.0.0/16, Direct
            IP-CIDR, 10.0.0.0/8, Direct
            IP-CIDR, 172.16.0.0/12, Direct
            IP-CIDR, 127.0.0.0/8, Direct
            IP-CIDR, 169.254.0.0/16, Direct
            """
        } else {
            ""
        }
        
        return """
            [General]
            loglevel = ${config.vpnConfig.logLevel}
            logoutput = console
            dns-server = ${config.vpnConfig.dnsServer}
            routing-domain-resolve = $routingDomainResolveStr
            tun-fd = $tunFd
            
            [Proxy]
            $proxyLine
            Direct = direct
            
            [Rule]
            $lanRules
            FINAL, ${selectedProxy.name}
            
            [Host]
        """.trimIndent()
    }
    
    /**
     * 判断应用是否应该被排除（不走VPN）
     * 注意：当前VPN配置逻辑已经直接在LeafVpnService中处理，此方法保留以备将来使用
     */
    fun shouldExcludeApp(packageName: String): Boolean {
        val config = getConfig()
        val isInList = config.appFilterConfig.selectedApps.contains(packageName)
        
        return if (config.appFilterConfig.isWhitelistMode) {
            // 白名单模式：不在列表中的应用需要排除
            !isInList
        } else {
            // 黑名单模式：在列表中的应用需要排除
            isInList
        }
    }
}