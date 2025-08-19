package moe.rikaaa0928.rileaf.data

import android.content.Context
import android.os.Build
import java.util.Locale

enum class SupportedLanguage(val code: String, val displayName: String, val locale: Locale) {
    SYSTEM("system", "跟随系统", Locale.getDefault()),
    CHINESE("zh", "简体中文", Locale.SIMPLIFIED_CHINESE),
    ENGLISH("en", "English", Locale.ENGLISH);

    companion object {
        fun fromCode(code: String): SupportedLanguage {
            return values().find { it.code == code } ?: SYSTEM
        }
    }
}

class LanguageManager(private val configManager: ConfigManager) {
    
    fun getCurrentLanguage(): SupportedLanguage {
        val config = configManager.getConfig()
        return SupportedLanguage.fromCode(config.appSettings.languageConfig.languageCode)
    }
    
    fun setLanguage(language: SupportedLanguage) {
        val config = configManager.getConfig()
        val newConfig = config.copy(
            appSettings = config.appSettings.copy(
                languageConfig = LanguageConfig(languageCode = language.code)
            )
        )
        configManager.saveConfig(newConfig)
    }
    
    fun getEffectiveLocale(): Locale {
        val currentLanguage = getCurrentLanguage()
        return when (currentLanguage) {
            SupportedLanguage.SYSTEM -> getSystemLocale()
            else -> currentLanguage.locale
        }
    }
    
    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.content.res.Resources.getSystem().configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            android.content.res.Resources.getSystem().configuration.locale
        }
    }
    
    fun updateContextLocale(context: Context): Context {
        val locale = getEffectiveLocale()
        val config = context.resources.configuration
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    fun getAllSupportedLanguages(): List<SupportedLanguage> {
        return SupportedLanguage.values().toList()
    }
}