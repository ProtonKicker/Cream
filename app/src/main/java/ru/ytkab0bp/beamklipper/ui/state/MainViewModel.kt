package ru.ytkab0bp.beamklipper.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val instances: StateFlow<List<KlipperInstance>> = AppState.instances
    val instanceStates: StateFlow<Map<String, KlipperInstance.State>> = AppState.instanceStates
    val webState: StateFlow<KlipperInstance.State> = AppState.webState
    val webFrontend: StateFlow<String> = AppState.webFrontend

    val anyRunning: Boolean
        get() = instances.value.any {
            it.getState() == KlipperInstance.State.RUNNING ||
                it.getState() == KlipperInstance.State.STARTING
        }

    fun toggle(instance: KlipperInstance) {
        val state = instance.getState()
        if (state == KlipperInstance.State.STARTING || state == KlipperInstance.State.STOPPING) return
        if (state == KlipperInstance.State.IDLE) {
            if (!KlipperInstance.hasFreeSlots()) return
            instance.start()
        } else {
            instance.stop()
            if (instance.autostart) {
                instance.autostart = false
                KlipperApp.DATABASE.update(instance)
            }
        }
    }

    fun runStopAll() {
        val instances = instances.value
        if (instances.isEmpty()) return
        val anyActive = instances.any {
            it.getState() == KlipperInstance.State.RUNNING ||
                it.getState() == KlipperInstance.State.STARTING
        }
        if (anyActive) {
            for (inst in instances) {
                val state = inst.getState()
                if (state == KlipperInstance.State.RUNNING || state == KlipperInstance.State.STARTING) {
                    if (state != KlipperInstance.State.STOPPING) {
                        inst.stop()
                        if (inst.autostart) {
                            inst.autostart = false
                            KlipperApp.DATABASE.update(inst)
                        }
                    }
                }
            }
        } else {
            for (inst in instances) {
                if (inst.getState() == KlipperInstance.State.IDLE) {
                    if (!KlipperInstance.hasFreeSlots()) return
                    inst.start()
                }
            }
        }
    }

    fun delete(instance: KlipperInstance) {
        instance.stop()
        viewModelScope.launch(Dispatchers.IO) {
            KlipperApp.DATABASE.delete(instance)
        }
    }
}
