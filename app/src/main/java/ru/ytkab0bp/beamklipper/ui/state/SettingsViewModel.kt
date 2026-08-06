package ru.ytkab0bp.beamklipper.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.utils.Prefs
import java.io.File

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    val engine: StateFlow<String> = AppState.engine
    val webFrontend: StateFlow<String> = AppState.webFrontend
    val usbNaming: StateFlow<Int> = AppState.usbNaming
    val cameraEnabled: StateFlow<Boolean> = AppState.cameraEnabled
    val appLanguage: StateFlow<String> = AppState.appLanguage
    val appTheme: StateFlow<String> = AppState.appTheme

    fun cycleEngine() {
        val next = if (Prefs.engine == Prefs.ENGINE_KLIPPER) Prefs.ENGINE_KALICO else Prefs.ENGINE_KLIPPER
        if (next == Prefs.ENGINE_KALICO &&
            !File(KlipperApp.INSTANCE.filesDir, "kalico/klippy/klippy.py").exists()
        ) {
            return
        }
        Prefs.engine = next
    }

    fun setEngine(engine: String) {
        if (engine == Prefs.ENGINE_KALICO &&
            !File(KlipperApp.INSTANCE.filesDir, "kalico/klippy/klippy.py").exists()
        ) {
            return
        }
        Prefs.engine = engine
    }

    fun cycleFrontend() {
        Prefs.webFrontend =
            if (Prefs.webFrontend == Prefs.FRONTEND_FLUIDD) Prefs.FRONTEND_MAINSAIL else Prefs.FRONTEND_FLUIDD
    }

    fun setFrontend(frontend: String) {
        Prefs.webFrontend = frontend
    }

    fun cycleUsbNaming() {
        Prefs.usbDeviceNaming =
            if (Prefs.usbDeviceNaming == Prefs.USB_DEVICE_NAMING_BY_PATH) Prefs.USB_DEVICE_NAMING_BY_VID_PID
            else Prefs.USB_DEVICE_NAMING_BY_PATH
    }

    fun setCameraEnabled(enabled: Boolean) {
        Prefs.isCameraEnabled = enabled
        KlipperInstance.onCameraConfigChanged(enabled)
    }

    fun refreshCameraSwitch(granted: Boolean) {
        if (granted) {
            Prefs.isCameraEnabled = true
            KlipperInstance.onCameraConfigChanged(true)
        }
    }

    fun engineTitle(engine: String): String = KlipperApp.INSTANCE.getString(
        if (engine == Prefs.ENGINE_KALICO) ru.ytkab0bp.beamklipper.R.string.Kalico
        else ru.ytkab0bp.beamklipper.R.string.Klipper
    )

    fun frontendTitle(frontend: String): String = KlipperApp.INSTANCE.getString(
        if (frontend == Prefs.FRONTEND_FLUIDD) ru.ytkab0bp.beamklipper.R.string.Fluidd
        else ru.ytkab0bp.beamklipper.R.string.Mainsail
    )

    fun usbNamingTitle(naming: Int): String = KlipperApp.INSTANCE.getString(
        if (naming == Prefs.USB_DEVICE_NAMING_BY_PATH) ru.ytkab0bp.beamklipper.R.string.USBDeviceNamingByPath
        else ru.ytkab0bp.beamklipper.R.string.USBDeviceNamingByVidPid
    )

    fun languageTitle(language: String): String = KlipperApp.INSTANCE.getString(
        when (language) {
            Prefs.LANGUAGE_ENGLISH -> ru.ytkab0bp.beamklipper.R.string.LanguageEnglish
            Prefs.LANGUAGE_RUSSIAN -> ru.ytkab0bp.beamklipper.R.string.LanguageRussian
            Prefs.LANGUAGE_CHINESE_SIMPLIFIED -> ru.ytkab0bp.beamklipper.R.string.LanguageChineseSimplified
            Prefs.LANGUAGE_CHINESE_TRADITIONAL -> ru.ytkab0bp.beamklipper.R.string.LanguageChineseTraditional
            else -> ru.ytkab0bp.beamklipper.R.string.LanguageSystem
        }
    )

    fun themeTitle(theme: String): String = KlipperApp.INSTANCE.getString(
        when (theme) {
            Prefs.THEME_LIGHT -> ru.ytkab0bp.beamklipper.R.string.ThemeLight
            Prefs.THEME_DARK -> ru.ytkab0bp.beamklipper.R.string.ThemeDark
            else -> ru.ytkab0bp.beamklipper.R.string.ThemeSystem
        }
    )
}
