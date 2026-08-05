package ru.ytkab0bp.beamklipper.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Region
import android.hardware.usb.UsbManager
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.NestedScrollView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.MainActivity
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.events.EngineChangedEvent
import ru.ytkab0bp.beamklipper.events.WebFrontendChangedEvent
import ru.ytkab0bp.beamklipper.serial.KlipperProbeTable
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager
import ru.ytkab0bp.beamklipper.utils.Prefs
import ru.ytkab0bp.beamklipper.utils.ViewUtils
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceSwitchView
import java.io.File

class PreferencesCardView(context: Context) : FrameLayout(context) {
    companion object {
        private const val MIN_HEIGHT_DP = 64
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dimmPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal val header: LinearLayout
    private val title: TextView
    private var progress = 0f
    private val path = Path()
    private val contentContainer: FrameLayout
    private lateinit var frontendValueView: TextView
    private lateinit var engineValueView: TextView
    private lateinit var cameraSwitch: PreferenceSwitchView
    private lateinit var usbNamingValueView: TextView
    private lateinit var scrollView: NestedScrollView

    init {
        dimmPaint.color = Color.BLACK
        outlinePaint.style = Paint.Style.FILL
        outlinePaint.color = ViewUtils.resolveColor(context, R.attr.cardOutlineColor)
        bgPaint.color = ViewUtils.resolveColor(context, android.R.attr.windowBackground)

        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        header = LinearLayout(context).apply {
            setPadding(ViewUtils.dp(21), ViewUtils.dp(8), ViewUtils.dp(21), 0)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        title = TextView(context).apply {
            setText(R.string.Settings)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            gravity = Gravity.CENTER
        }
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        ll.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(48)))
        header.setOnApplyWindowInsetsListener { v, insets ->
            val topInset = insets.systemWindowInsetTop
            v.setPadding(ViewUtils.dp(21), ViewUtils.dp(8) + topInset, ViewUtils.dp(21), 0)
            val lp = v.layoutParams as LinearLayout.LayoutParams
            lp.height = ViewUtils.dp(48) + topInset
            v.layoutParams = lp
            insets
        }

        contentContainer = FrameLayout(context)
        buildMainSettingsPage()
        ll.addView(contentContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        addView(ll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        setWillNotDraw(false)
    }

    private fun buildMainSettingsPage() {
        contentContainer.removeAllViews()

        scrollView = NestedScrollView(context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewUtils.dp(20), ViewUtils.dp(4), ViewUtils.dp(20), ViewUtils.dp(40))
        }

        content.addView(buildSectionHeader(R.string.EngineFrontend),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = ViewUtils.dp(8)
                bottomMargin = ViewUtils.dp(12)
                marginStart = ViewUtils.dp(4)
            })

        val row1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        row1.addView(buildSnippetCard(
            R.drawable.ic_memory_chip_outline_28,
            R.string.FirmwareEngine,
            { firmwareTitle(Prefs.engine) },
            { cycleEngine() }
        ), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = ViewUtils.dp(10)
        })
        row1.addView(buildSnippetCard(
            R.drawable.ic_sync_outline_28,
            R.string.WebFrontend,
            { frontendTitle(Prefs.webFrontend) },
            { cycleFrontend() }
        ), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(row1, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(28)
        })

        content.addView(buildSectionHeader(R.string.USB),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = ViewUtils.dp(12)
                marginStart = ViewUtils.dp(4)
            })
        val row2 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        row2.addView(buildSnippetCard(
            R.drawable.ic_usb_cable_28,
            R.string.USBDeviceNaming,
            { usbNamingTitle(Prefs.usbDeviceNaming) },
            { cycleUsbNaming() }
        ), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = ViewUtils.dp(10)
        })
        row2.addView(buildSnippetCard(
            R.drawable.ic_grid_layout_outline_28,
            R.string.ListUSB,
            null,
            { showListUsbDialog() }
        ), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(row2, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(28)
        })

        content.addView(buildSectionHeader(R.string.Camera),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = ViewUtils.dp(8)
                marginStart = ViewUtils.dp(4)
            })
        cameraSwitch = buildSwitchRow(R.string.EnableCamera, Prefs.isCameraEnabled, { v, checked ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(v.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.CAMERA), 0)
                cameraSwitch.isChecked = false
                return@buildSwitchRow
            }
            Prefs.isCameraEnabled = checked
            KlipperInstance.onCameraConfigChanged(checked)
        })
        content.addView(cameraSwitch, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(28)
        })

        content.addView(buildSectionHeader(R.string.Other),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = ViewUtils.dp(12)
                marginStart = ViewUtils.dp(4)
            })
        val row4 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val ctx4 = context
        val isLauncher = ctx4 is MainActivity && ctx4.isCurrentLauncher()
        if (isLauncher) {
            row4.addView(buildSnippetCard(
                R.drawable.ic_services_outline_28,
                R.string.SystemSettings,
                null,
                { ctx4.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            ), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = ViewUtils.dp(10)
            })
        }
        row4.addView(buildSnippetCard(
            R.drawable.ic_mcu_firmware_outline_28,
            R.string.OtherGetFirmware,
            null,
            { QRCodeAlertDialog(context, "https://github.com/utkabobr/klipper/releases/tag/prebuilt-v0.12.0").show() }
        ), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(row4, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(28)
        })

        content.addView(buildSectionHeader(R.string.AppSettings),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = ViewUtils.dp(12)
                marginStart = ViewUtils.dp(4)
            })
        val appRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        appRow.addView(buildSnippetCard(
            R.drawable.ic_globe_outline_28,
            R.string.AppLanguage,
            { languageTitle(Prefs.appLanguage) },
            {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.AppLanguage)
                    .setItems(
                        arrayOf(
                            KlipperApp.INSTANCE.getString(R.string.LanguageSystem),
                            KlipperApp.INSTANCE.getString(R.string.LanguageEnglish),
                            KlipperApp.INSTANCE.getString(R.string.LanguageRussian),
                            KlipperApp.INSTANCE.getString(R.string.LanguageChineseSimplified),
                            KlipperApp.INSTANCE.getString(R.string.LanguageChineseTraditional)
                        )
                    ) { _, which ->
                        Prefs.appLanguage = when (which) {
                            0 -> Prefs.LANGUAGE_SYSTEM
                            1 -> Prefs.LANGUAGE_ENGLISH
                            2 -> Prefs.LANGUAGE_RUSSIAN
                            3 -> Prefs.LANGUAGE_CHINESE_SIMPLIFIED
                            else -> Prefs.LANGUAGE_CHINESE_TRADITIONAL
                        }
                        Prefs.applyAppLanguage()
                        (context as? AppCompatActivity)?.recreate()
                    }
                    .show()
            }
        ), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = ViewUtils.dp(10)
        })
        appRow.addView(buildSnippetCard(
            R.drawable.ic_moon_outline_28,
            R.string.AppTheme,
            { themeTitle(Prefs.appTheme) },
            {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.AppTheme)
                    .setItems(
                        arrayOf(
                            KlipperApp.INSTANCE.getString(R.string.ThemeSystem),
                            KlipperApp.INSTANCE.getString(R.string.ThemeLight),
                            KlipperApp.INSTANCE.getString(R.string.ThemeDark)
                        )
                    ) { _, which ->
                        Prefs.appTheme = when (which) {
                            0 -> Prefs.THEME_SYSTEM
                            1 -> Prefs.THEME_LIGHT
                            else -> Prefs.THEME_DARK
                        }
                        Prefs.applyAppTheme()
                    }
                    .show()
            }
        ), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(appRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        scrollView.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        contentContainer.addView(scrollView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun buildAppSettingsPage() {
        contentContainer.removeAllViews()

        scrollView = NestedScrollView(context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewUtils.dp(20), ViewUtils.dp(4), ViewUtils.dp(20), ViewUtils.dp(40))
        }

        val backRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ViewUtils.dp(4), ViewUtils.dp(8), ViewUtils.dp(4), ViewUtils.dp(8))
            background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
            setOnClickListener { buildMainSettingsPage() }
        }
        val backIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_chevron_up_outline_28)
            rotation = -90f
            imageTintList = android.content.res.ColorStateList.valueOf(
                ViewUtils.resolveColor(context, android.R.attr.textColorPrimary)
            )
            layoutParams = LinearLayout.LayoutParams(ViewUtils.dp(24), ViewUtils.dp(24)).apply {
                marginEnd = ViewUtils.dp(12)
            }
        }
        backRow.addView(backIcon)
        val backText = TextView(context).apply {
            setText(R.string.AppSettings)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        }
        backRow.addView(backText)
        content.addView(backRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(24)
        })

        content.addView(buildSectionHeader(R.string.AppLanguage),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = ViewUtils.dp(12)
                marginStart = ViewUtils.dp(4)
            })

        content.addView(buildValueRow(R.string.AppLanguage, languageTitle(Prefs.appLanguage)) {
            MaterialAlertDialogBuilder(it.context)
                .setTitle(R.string.AppLanguage)
                .setItems(
                    arrayOf(
                        KlipperApp.INSTANCE.getString(R.string.LanguageSystem),
                        KlipperApp.INSTANCE.getString(R.string.LanguageEnglish),
                        KlipperApp.INSTANCE.getString(R.string.LanguageRussian),
                        KlipperApp.INSTANCE.getString(R.string.LanguageChineseSimplified),
                        KlipperApp.INSTANCE.getString(R.string.LanguageChineseTraditional)
                    )
                ) { _, which ->
                    Prefs.appLanguage = when (which) {
                        0 -> Prefs.LANGUAGE_SYSTEM
                        1 -> Prefs.LANGUAGE_ENGLISH
                        2 -> Prefs.LANGUAGE_RUSSIAN
                        3 -> Prefs.LANGUAGE_CHINESE_SIMPLIFIED
                        else -> Prefs.LANGUAGE_CHINESE_TRADITIONAL
                    }
                    Prefs.applyAppLanguage()
                    buildAppSettingsPage()
                    (it.context as? AppCompatActivity)?.recreate()
                }
                .show()
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(28)
        })

        content.addView(buildSectionHeader(R.string.AppTheme),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = ViewUtils.dp(12)
                marginStart = ViewUtils.dp(4)
            })
        content.addView(buildValueRow(R.string.AppTheme, themeTitle(Prefs.appTheme)) {
            MaterialAlertDialogBuilder(it.context)
                .setTitle(R.string.AppTheme)
                .setItems(
                    arrayOf(
                        KlipperApp.INSTANCE.getString(R.string.ThemeSystem),
                        KlipperApp.INSTANCE.getString(R.string.ThemeLight),
                        KlipperApp.INSTANCE.getString(R.string.ThemeDark)
                    )
                ) { _, which ->
                    Prefs.appTheme = when (which) {
                        0 -> Prefs.THEME_SYSTEM
                        1 -> Prefs.THEME_LIGHT
                        else -> Prefs.THEME_DARK
                    }
                    Prefs.applyAppTheme()
                    buildAppSettingsPage()
                }
                .show()
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        scrollView.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        contentContainer.addView(scrollView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun buildSectionHeader(titleRes: Int): View {
        return TextView(context).apply {
            setText(titleRes)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            letterSpacing = 0.06f
            isAllCaps = true
        }
    }

    private fun buildSnippetCard(iconRes: Int, titleRes: Int, valueProvider: (() -> String)?, onClick: (() -> Unit)?): View {
        val card = MaterialCardView(context).apply {
            radius = ViewUtils.dp(24).toFloat()
            strokeWidth = 0
            cardElevation = 0f
            setCardBackgroundColor(ViewUtils.resolveColor(context, R.attr.colorSurfaceContainerLow))
        }
        val inner = FrameLayout(context).apply {
            if (onClick != null) {
                background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
                setOnClickListener { onClick() }
            }
            setPadding(ViewUtils.dp(18), ViewUtils.dp(18), ViewUtils.dp(18), ViewUtils.dp(18))
        }
        val vertical = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val icon = ImageView(context).apply {
            setImageResource(iconRes)
            imageTintList = android.content.res.ColorStateList.valueOf(
                ViewUtils.resolveColor(context, android.R.attr.textColorPrimary)
            )
            layoutParams = LinearLayout.LayoutParams(ViewUtils.dp(32), ViewUtils.dp(32)).apply {
                bottomMargin = ViewUtils.dp(20)
            }
        }
        vertical.addView(icon)
        val titleTv = TextView(context).apply {
            setText(titleRes)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            setLineSpacing(0f, 1.1f)
        }
        vertical.addView(titleTv)
        if (valueProvider != null) {
            val valueTv = object : TextView(context) {
                override fun onAttachedToWindow() {
                    super.onAttachedToWindow()
                    text = valueProvider()
                }
            }.apply {
                id = generateViewId()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
                setPadding(0, ViewUtils.dp(2), 0, 0)
            }
            when (titleRes) {
                R.string.FirmwareEngine -> engineValueView = valueTv
                R.string.WebFrontend -> frontendValueView = valueTv
                R.string.USBDeviceNaming -> usbNamingValueView = valueTv
            }
            vertical.addView(valueTv)
        }
        inner.addView(vertical, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        card.addView(inner)
        return card
    }

    private fun buildSwitchRow(titleRes: Int, checked: Boolean, onChecked: (View, Boolean) -> Unit): PreferenceSwitchView {
        return PreferenceSwitchView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(56))
            minimumHeight = ViewUtils.dp(56)
            setPadding(ViewUtils.dp(21), 0, ViewUtils.dp(16), 0)
            val titleStr = context.getString(titleRes)
            bind(titleStr, null, checked)
            setOnClickListener {
                val newChecked = !isChecked
                isChecked = newChecked
                onChecked(this, newChecked)
            }
        }
    }

    private fun buildValueRow(titleRes: Int, value: String, onClick: (View) -> Unit): View {
        val fl = FrameLayout(context).apply {
            background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
            setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0)
            setOnClickListener(onClick)
            val card = MaterialCardView(context).apply {
                radius = ViewUtils.dp(22).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(ViewUtils.resolveColor(context, R.attr.colorSurfaceContainerLow))
            }
            val inner = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(ViewUtils.dp(20), ViewUtils.dp(14), ViewUtils.dp(20), ViewUtils.dp(14))
            }
            val titleTv = TextView(context).apply {
                setText(titleRes)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            inner.addView(titleTv)
            val valueTv = TextView(context).apply {
                text = value
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = ViewUtils.dp(4)
                }
            }
            inner.addView(valueTv)
            val chev = ImageView(context).apply {
                setImageResource(R.drawable.ic_chevron_up_outline_28)
                rotation = 90f
                imageTintList = android.content.res.ColorStateList.valueOf(
                    ViewUtils.resolveColor(context, android.R.attr.textColorSecondary)
                )
                layoutParams = LinearLayout.LayoutParams(ViewUtils.dp(18), ViewUtils.dp(18))
            }
            inner.addView(chev)
            card.addView(inner)
            addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return fl
    }

    private fun frontendTitle(frontend: String): String {
        val resId = when (frontend) {
            Prefs.FRONTEND_FLUIDD -> R.string.Fluidd
            else -> R.string.Mainsail
        }
        return KlipperApp.INSTANCE.getString(resId)
    }

    private fun firmwareTitle(engine: String): String {
        return KlipperApp.INSTANCE.getString(
            if (engine == Prefs.ENGINE_KALICO) R.string.Kalico else R.string.Klipper
        )
    }

    private fun languageTitle(language: String): String {
        return KlipperApp.INSTANCE.getString(
            when (language) {
                Prefs.LANGUAGE_ENGLISH -> R.string.LanguageEnglish
                Prefs.LANGUAGE_RUSSIAN -> R.string.LanguageRussian
                Prefs.LANGUAGE_CHINESE_SIMPLIFIED -> R.string.LanguageChineseSimplified
                Prefs.LANGUAGE_CHINESE_TRADITIONAL -> R.string.LanguageChineseTraditional
                else -> R.string.LanguageSystem
            }
        )
    }

    private fun themeTitle(theme: String): String {
        return KlipperApp.INSTANCE.getString(
            when (theme) {
                Prefs.THEME_LIGHT -> R.string.ThemeLight
                Prefs.THEME_DARK -> R.string.ThemeDark
                else -> R.string.ThemeSystem
            }
        )
    }

    private fun usbNamingTitle(naming: Int): String {
        return KlipperApp.INSTANCE.getString(
            if (naming == Prefs.USB_DEVICE_NAMING_BY_PATH) R.string.USBDeviceNamingByPath else R.string.USBDeviceNamingByVidPid
        )
    }

    private fun cycleEngine() {
        val next = if (Prefs.engine == Prefs.ENGINE_KLIPPER) Prefs.ENGINE_KALICO else Prefs.ENGINE_KLIPPER
        if (next == Prefs.ENGINE_KALICO &&
            !File(KlipperApp.INSTANCE.filesDir, "kalico/klippy/klippy.py").exists()
        ) {
            return
        }
        Prefs.engine = next
        if (::engineValueView.isInitialized) engineValueView.text = firmwareTitle(next)
    }

    private fun cycleFrontend() {
        val next = if (Prefs.webFrontend == Prefs.FRONTEND_FLUIDD) Prefs.FRONTEND_MAINSAIL else Prefs.FRONTEND_FLUIDD
        Prefs.webFrontend = next
        if (::frontendValueView.isInitialized) frontendValueView.text = frontendTitle(next)
    }

    private fun cycleUsbNaming() {
        val next = if (Prefs.usbDeviceNaming == Prefs.USB_DEVICE_NAMING_BY_PATH) Prefs.USB_DEVICE_NAMING_BY_VID_PID else Prefs.USB_DEVICE_NAMING_BY_PATH
        Prefs.usbDeviceNaming = next
        if (::usbNamingValueView.isInitialized) usbNamingValueView.text = usbNamingTitle(next)
    }

    private fun showEngineDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.FirmwareEngine)
            .setItems(
                arrayOf(
                    KlipperApp.INSTANCE.getString(R.string.Klipper),
                    KlipperApp.INSTANCE.getString(R.string.Kalico)
                )
            ) { _, which ->
                val engine = if (which == 0) Prefs.ENGINE_KLIPPER else Prefs.ENGINE_KALICO
                if (engine == Prefs.ENGINE_KALICO &&
                    !File(KlipperApp.INSTANCE.filesDir, "kalico/klippy/klippy.py").exists()
                ) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.Error)
                        .setMessage(R.string.EngineNotBundled)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    return@setItems
                }
                Prefs.engine = engine
                engineValueView.text = firmwareTitle(engine)
                if (KlipperInstance.getInstances().any { inst -> inst.getState() == KlipperInstance.State.RUNNING }) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.FirmwareEngine)
                        .setMessage(R.string.EngineRestartRequired)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
            .show()
    }

    private fun showFrontendDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.WebFrontend)
            .setItems(
                arrayOf(
                    KlipperApp.INSTANCE.getString(R.string.Fluidd),
                    KlipperApp.INSTANCE.getString(R.string.Mainsail)
                )
            ) { _, which ->
                Prefs.webFrontend = when (which) {
                    0 -> Prefs.FRONTEND_FLUIDD
                    else -> Prefs.FRONTEND_MAINSAIL
                }
                frontendValueView.text = frontendTitle(Prefs.webFrontend)
            }
            .show()
    }

    private fun showUsbNamingDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.USBDeviceNaming)
            .setItems(arrayOf(
                KlipperApp.INSTANCE.getString(R.string.USBDeviceNamingByPath),
                KlipperApp.INSTANCE.getString(R.string.USBDeviceNamingByVidPid)
            ), DialogInterface.OnClickListener { _, which ->
                Prefs.usbDeviceNaming = which
                usbNamingValueView.text = usbNamingTitle(which)
            })
            .show()
    }

    private fun showListUsbDialog() {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val list = mutableListOf<String>()
        for (dev in manager.deviceList.values) {
            val drv = KlipperProbeTable.getInstance().findDriver(dev)
            list.add(
                Integer.toHexString(dev.vendorId) + "/" + Integer.toHexString(dev.productId) +
                        " - " + dev.deviceName +
                        (if (drv != null) " - " + drv.name + "\n" +
                                File(KlipperApp.INSTANCE.filesDir, "serial/" + UsbSerialManager.getUID(dev)).absolutePath else "")
            )
        }
        val b = MaterialAlertDialogBuilder(context).setTitle(R.string.ListUSBTitle)
        if (list.isEmpty()) {
            b.setMessage(R.string.ListUSBNoDevices)
        } else {
            b.setItems(list.toTypedArray(), null)
        }
        b.setPositiveButton(android.R.string.ok, null).show()
    }

    fun refreshCameraSwitch(granted: Boolean) {
        if (granted) {
            cameraSwitch.isChecked = true
            Prefs.isCameraEnabled = true
            KlipperInstance.onCameraConfigChanged(true)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        KlipperApp.EVENT_BUS.registerListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        KlipperApp.EVENT_BUS.unregisterListener(this)
    }

    fun onEvent(e: EngineChangedEvent) {
        if (::engineValueView.isInitialized) engineValueView.text = firmwareTitle(Prefs.engine)
    }
    fun onEvent(e: WebFrontendChangedEvent) {
        if (::frontendValueView.isInitialized) frontendValueView.text = frontendTitle(Prefs.webFrontend)
    }

    override fun draw(canvas: Canvas) {
        val p = Math.max(0f, Math.min(1f, progress))
        val radius = (1f - p) * ViewUtils.dp(32)
        path.rewind()
        path.addRoundRect(
            0f,
            0f,
            width.toFloat(),
            height + radius,
            radius, radius,
            Path.Direction.CW
        )
        canvas.save()
        canvas.clipPath(path)
        canvas.drawPaint(bgPaint)
        super.draw(canvas)
        canvas.restore()
    }

    private fun invalidateProgress() {
        val p = Math.max(0f, Math.min(1f, progress))
        alpha = p

        if (context is MainActivity) {
            val w: Window = (context as MainActivity).window
            w.navigationBarColor = ColorUtils.blendARGB(
                ViewUtils.resolveColor(context, R.attr.navbarColor),
                ViewUtils.resolveColor(context, android.R.attr.windowBackground),
                p
            )
        }
    }

    fun setProgress(progress: Float) {
        val prev = this.progress
        this.progress = progress
        if (prev < 1f && progress >= 1f) {
            scrollView.post { scrollView.scrollTo(0, 0) }
        }
        invalidateProgress()
        invalidate()
    }
}
