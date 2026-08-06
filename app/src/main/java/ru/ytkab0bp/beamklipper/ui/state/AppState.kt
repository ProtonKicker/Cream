package ru.ytkab0bp.beamklipper.ui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.events.EngineChangedEvent
import ru.ytkab0bp.beamklipper.events.InstanceCreatedEvent
import ru.ytkab0bp.beamklipper.events.InstanceDestroyedEvent
import ru.ytkab0bp.beamklipper.events.InstancesRefreshedEvent
import ru.ytkab0bp.beamklipper.events.InstanceStateChangedEvent
import ru.ytkab0bp.beamklipper.events.InstanceUpdatedEvent
import ru.ytkab0bp.beamklipper.events.WebFrontendChangedEvent
import ru.ytkab0bp.beamklipper.events.WebStateChangedEvent
import ru.ytkab0bp.beamklipper.utils.Prefs
import ru.ytkab0bp.eventbus.EventHandler

object AppState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun <T, M : MutableStateFlow<T>> M.distinct(): StateFlow<T> = this
        .let { src -> kotlinx.coroutines.flow.flow<T> { src.collect { emit(it) } } }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), this.value)

    private inline fun <T, M : MutableStateFlow<T>> M.distinct(
        crossinline areEquivalent: (a: T, b: T) -> Boolean
    ): StateFlow<T> = this
        .let { src -> kotlinx.coroutines.flow.flow<T> { src.collect { emit(it) } } }
        .distinctUntilChanged { a, b -> areEquivalent(a, b) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), this.value)

    private val _instances = MutableStateFlow<List<KlipperInstance>>(emptyList())
    val instances: StateFlow<List<KlipperInstance>> = _instances.distinct { a, b ->
        a.size == b.size && a.asSequence().zip(b.asSequence()).all { (x, y) ->
            val xi = x.id ?: x.name
            val yi = y.id ?: y.name
            xi === yi || xi == yi
        }
    }

    private val _instanceStates = MutableStateFlow<Map<String, KlipperInstance.State>>(emptyMap())
    val instanceStates: StateFlow<Map<String, KlipperInstance.State>> = _instanceStates.distinct()

    private val _webState = MutableStateFlow(KlipperInstance.State.IDLE)
    val webState: StateFlow<KlipperInstance.State> = _webState.distinct()

    private val _webFrontend = MutableStateFlow(Prefs.webFrontend)
    val webFrontend: StateFlow<String> = _webFrontend.distinct()

    private val _engine = MutableStateFlow(Prefs.engine)
    val engine: StateFlow<String> = _engine.distinct()

    private val _usbNaming = MutableStateFlow(Prefs.usbDeviceNaming)
    val usbNaming: StateFlow<Int> = _usbNaming.distinct()

    private val _cameraEnabled = MutableStateFlow(Prefs.isCameraEnabled)
    val cameraEnabled: StateFlow<Boolean> = _cameraEnabled.distinct()

    private val _appLanguage = MutableStateFlow(Prefs.appLanguage)
    val appLanguage: StateFlow<String> = _appLanguage.distinct()

    private val _appTheme = MutableStateFlow(Prefs.appTheme)
    val appTheme: StateFlow<String> = _appTheme.distinct()

    private var started = false

    fun start() {
        if (started) return
        started = true
        _appLanguage.value = Prefs.appLanguage
        _appTheme.value = Prefs.appTheme
        _usbNaming.value = Prefs.usbDeviceNaming
        _cameraEnabled.value = Prefs.isCameraEnabled
        KlipperApp.EVENT_BUS.registerListener(this)
        refreshInstances()
    }

    fun stop() {
        if (!started) return
        started = false
        KlipperApp.EVENT_BUS.unregisterListener(this)
    }

    private fun refreshInstances() {
        val list = KlipperInstance.getInstances()
        _instances.value = list
        _instanceStates.value = list.associate { (it.id ?: it.name) to it.getState() }
    }

    @EventHandler(runOnMainThread = true)
    fun onInstancesRefreshed(e: InstancesRefreshedEvent) {
        refreshInstances()
    }

    @EventHandler(runOnMainThread = true)
    fun onInstanceCreated(e: InstanceCreatedEvent) {
        refreshInstances()
    }

    @EventHandler(runOnMainThread = true)
    fun onInstanceUpdated(e: InstanceUpdatedEvent) {
        refreshInstances()
    }

    @EventHandler(runOnMainThread = true)
    fun onInstanceDestroyed(e: InstanceDestroyedEvent) {
        refreshInstances()
    }

    @EventHandler(runOnMainThread = true)
    fun onInstanceStateChanged(e: InstanceStateChangedEvent) {
        _instanceStates.update { it + (e.id to e.state) }
    }

    @EventHandler(runOnMainThread = true)
    fun onWebStateChanged(e: WebStateChangedEvent) {
        _webState.value = e.state
    }

    @EventHandler(runOnMainThread = true)
    fun onWebFrontendChanged(e: WebFrontendChangedEvent) {
        _webFrontend.value = Prefs.webFrontend
    }

    @EventHandler(runOnMainThread = true)
    fun onEngineChanged(e: EngineChangedEvent) {
        _engine.value = Prefs.engine
    }

    fun updateUsbNaming() {
        _usbNaming.value = Prefs.usbDeviceNaming
    }

    fun updateCameraEnabled() {
        _cameraEnabled.value = Prefs.isCameraEnabled
    }

    fun updateAppLanguage() {
        _appLanguage.value = Prefs.appLanguage
    }

    fun updateAppTheme() {
        _appTheme.value = Prefs.appTheme
    }
}
