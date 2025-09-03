package moe.rikaaa0928.rileaf.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VpnState {
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED
}

object VpnStateRepository {
    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState = _vpnState.asStateFlow()

    fun updateState(newState: VpnState) {
        _vpnState.value = newState
    }
}
