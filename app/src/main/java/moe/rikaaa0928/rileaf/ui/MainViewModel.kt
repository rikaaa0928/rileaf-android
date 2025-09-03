package moe.rikaaa0928.rileaf.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import moe.rikaaa0928.rileaf.data.ConfigManager
import moe.rikaaa0928.rileaf.data.VpnState
import moe.rikaaa0928.rileaf.data.VpnStateRepository

class MainViewModel(private val configManager: ConfigManager) : ViewModel() {
    val vpnState: StateFlow<VpnState> = VpnStateRepository.vpnState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VpnState.DISCONNECTED
        )

    private val _currentProxyName = mutableStateOf("")
    val currentProxyName: State<String> = _currentProxyName

    init {
        refreshCurrentProxy()
    }

    fun refreshCurrentProxy() {
        val config = configManager.getConfig()
        val selectedProxy = config.proxies.find { it.id == config.selectedProxyId }
        _currentProxyName.value = selectedProxy?.name ?: ""
    }
}

class MainViewModelFactory(private val configManager: ConfigManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(configManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
