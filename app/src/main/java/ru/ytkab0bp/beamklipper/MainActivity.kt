package ru.ytkab0bp.beamklipper

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.ytkab0bp.beamklipper.events.*
import ru.ytkab0bp.beamklipper.serial.KlipperProbeTable
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager
import ru.ytkab0bp.beamklipper.utils.Prefs
import ru.ytkab0bp.beamklipper.utils.ViewUtils
import ru.ytkab0bp.beamklipper.view.*
import ru.ytkab0bp.eventbus.EventHandler
import java.io.File

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_NOTIFICATIONS = 100
        private const val REQUEST_CAMERA = 200
        private const val VIEW_TYPE_WEB = 0
        private const val VIEW_TYPE_INSTANCE = 1
        private val NOTIFY_LIVE = Any()
    }

    private lateinit var homeView: HomeView
    private lateinit var mainPage: FrameLayout
    private lateinit var badgesLayout: FrameLayout
    private lateinit var gearBtn: MaterialCardView
    private lateinit var helpBtn: MaterialCardView

    private lateinit var instancesRecycler: RecyclerView
    private lateinit var instancesAdapter: RecyclerView.Adapter<*>
    private var instances = mutableListOf<KlipperInstance>()

    private lateinit var addButton: MaterialCardView
    private lateinit var runStopButton: MaterialCardView
    private lateinit var addIcon: ImageView
    private lateinit var runStopIcon: ImageView
    private lateinit var bottomButtonsWrap: FrameLayout

    private lateinit var preferencesView: PreferencesCardView
    private lateinit var helpView: HelpView

    private lateinit var noPermsLayout: MaterialCardView
    private lateinit var batteryRow: PermissionRowView
    private var notificationsRow: PermissionRowView? = null
    private var hideServicesChannelRow: PermissionRowView? = null
    private var brokenBySDCardRow: PermissionRowView? = null

    private var isTV = false
    private var isCurrentLauncher = false

    private var selectedInstanceForToggle: KlipperInstance? = null

    @SuppressLint("BatteryLife", "InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            !packageManager.hasSystemFeature("android.hardware.touchscreen") ||
            !packageManager.hasSystemFeature("android.hardware.telephony")
        ) {
            isTV = true
            PermissionsChecker.setIgnoreNotificationsChannel(true)
        }
        if (Build.MANUFACTURER.lowercase(java.util.Locale.ROOT).contains("meizu") ||
            Build.BRAND.lowercase(java.util.Locale.ROOT).contains("meizu")
        ) {
            PermissionsChecker.setIgnoreNotificationsChannel(true)
        }
        isCurrentLauncher = intent?.categories?.contains(Intent.CATEGORY_HOME) == true

        val root = FrameLayout(this)
        homeView = HomeView(this)
        buildSettingsHelpPages()
        buildMainPage()
        homeView.setProgressListener { invalidateHomeProgress(it) }
        root.addView(homeView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        buildPermissionScreen(root)
        root.setBackgroundColor(ViewUtils.resolveColor(this, android.R.attr.windowBackground))
        setContentView(root)

        processIntent(intent)
        instances = ArrayList(KlipperInstance.getInstances())
        instancesAdapter.notifyDataSetChanged()
        refreshBottomButtons()
        KlipperApp.EVENT_BUS.registerListener(this)

        if (Prefs.getLastCommit() != BuildConfig.COMMIT && KlipperApp.hasUpdateInfo) {
            Prefs.setLastCommit()
            ChangeLogBottomSheet(this@MainActivity).show()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (homeView.progress != HomeView.PAGE_MAIN) {
                    homeView.animateTo(HomeView.PAGE_MAIN)
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun buildMainPage() {
        mainPage = FrameLayout(this)

        badgesLayout = object : FrameLayout(this) {
            override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                super.onSizeChanged(w, h, oldw, oldh)
                invalidateHomeProgress(homeView.progress)
            }
        }.apply {
            clipChildren = false
            clipToPadding = false
        }
        gearBtn = buildTopTile(R.drawable.ic_settings_outline_28).apply {
            setOnClickListener { homeView.animateTo(HomeView.PAGE_SETTINGS) }
        }
        badgesLayout.addView(gearBtn, FrameLayout.LayoutParams(ViewUtils.dp(44), ViewUtils.dp(44)).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = ViewUtils.dp(10)
            leftMargin = ViewUtils.dp(16)
        })

        helpBtn = buildTopTile(R.drawable.ic_help_outline_28).apply {
            setOnClickListener { homeView.animateTo(HomeView.PAGE_HELP) }
        }
        badgesLayout.addView(helpBtn, FrameLayout.LayoutParams(ViewUtils.dp(44), ViewUtils.dp(44)).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = ViewUtils.dp(10)
            rightMargin = ViewUtils.dp(16)
        })

        val titleTv = TextView(this).apply {
            setText(R.string.AppName)
            setTextColor(ViewUtils.resolveColor(this@MainActivity, android.R.attr.textColorPrimary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        }
        badgesLayout.addView(titleTv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = ViewUtils.dp(18)
        })

        instancesRecycler = RecyclerView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutManager = LinearLayoutManager(this@MainActivity)
            itemAnimator = SmoothItemAnimator()
            clipToPadding = false
        }
        homeView.setScrollView(instancesRecycler)
        instancesAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v: View = when (viewType) {
                    VIEW_TYPE_WEB, VIEW_TYPE_INSTANCE -> KlipperInstanceView(this@MainActivity)
                    else -> throw IllegalStateException("Unknown viewType: $viewType")
                }
                return object : RecyclerView.ViewHolder(v) {}
            }

            @Suppress("UNCHECKED_CAST")
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
                if (payloads.contains(NOTIFY_LIVE) && getItemViewType(position) == VIEW_TYPE_INSTANCE) {
                    val view = holder.itemView as KlipperInstanceView
                    view.bind(instances[position - 1])
                    return
                }
                super.onBindViewHolder(holder, position, payloads)
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val view = holder.itemView as KlipperInstanceView
                when (getItemViewType(position)) {
                    VIEW_TYPE_WEB -> {
                        view.bindWeb()
                    }
                    VIEW_TYPE_INSTANCE -> {
                        val inst = instances[position - 1]
                        view.bind(inst)
                        view.setOnClickListener {
                            InstanceEditorBottomSheet.show(this@MainActivity, inst)
                        }
                        view.setOnLongClickListener {
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle(getString(R.string.InstanceDelete, inst.name))
                                .setMessage(R.string.InstanceDeleteConfirm)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    KlipperApp.appScope.launch(Dispatchers.IO) {
                                        KlipperApp.DATABASE.delete(inst)
                                    }
                                }
                                .show()
                            true
                        }
                    }
                }
            }

            override fun getItemCount(): Int = instances.size + 1
            override fun getItemViewType(position: Int): Int = if (position == 0) VIEW_TYPE_WEB else VIEW_TYPE_INSTANCE
        }
        instancesRecycler.adapter = instancesAdapter

        val contentPaddingTop = ViewUtils.dp(80)
        val contentPaddingBottom = ViewUtils.dp(200)
        instancesRecycler.setPadding(ViewUtils.dp(20), contentPaddingTop, ViewUtils.dp(20), contentPaddingBottom)

        val instancesWrap = FrameLayout(this)
        instancesWrap.addView(instancesRecycler, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        mainPage.addView(instancesWrap, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        bottomButtonsWrap = FrameLayout(this).apply {
            id = View.generateViewId()
            clipChildren = false
            clipToPadding = false
        }
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val gap = ViewUtils.dp(40)

        addButton = buildBigCreamButton(R.drawable.ic_add_outline_28).apply {
            setOnClickListener { InstanceEditorBottomSheet.show(this@MainActivity, null) }
        }
        addIcon = (addButton.getChildAt(0) as FrameLayout).getChildAt(0) as ImageView
        bottomRow.addView(addButton, LinearLayout.LayoutParams(ViewUtils.dp(108), ViewUtils.dp(108)).apply {
            marginEnd = gap
        })

        runStopButton = buildBigCreamButton(R.drawable.ic_play_28).apply {
            setOnClickListener { runStopAll() }
        }
        runStopIcon = (runStopButton.getChildAt(0) as FrameLayout).getChildAt(0) as ImageView
        bottomRow.addView(runStopButton, LinearLayout.LayoutParams(ViewUtils.dp(108), ViewUtils.dp(108)))

        bottomButtonsWrap.addView(bottomRow, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        mainPage.addView(bottomButtonsWrap, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewUtils.dp(108),
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            leftMargin = ViewUtils.dp(24)
            rightMargin = ViewUtils.dp(24)
            bottomMargin = ViewUtils.dp(60)
        })

        mainPage.addView(badgesLayout, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        mainPage.setOnApplyWindowInsetsListener { _, insets ->
            badgesLayout.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, 0)
            instancesRecycler.setPadding(
                ViewUtils.dp(20) + insets.systemWindowInsetLeft,
                contentPaddingTop + insets.systemWindowInsetTop,
                ViewUtils.dp(20) + insets.systemWindowInsetRight,
                ViewUtils.dp(180) + insets.systemWindowInsetBottom
            )
            val lp = bottomButtonsWrap.layoutParams as FrameLayout.LayoutParams
            lp.bottomMargin = ViewUtils.dp(60) + insets.systemWindowInsetBottom
            lp.leftMargin = ViewUtils.dp(24) + insets.systemWindowInsetLeft
            lp.rightMargin = ViewUtils.dp(24) + insets.systemWindowInsetRight
            bottomButtonsWrap.layoutParams = lp
            insets
        }

        homeView.addView(mainPage, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun buildTopTile(iconRes: Int): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = ViewUtils.dp(20).toFloat()
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(ViewUtils.resolveColor(this@MainActivity, android.R.attr.windowBackground))
        }
        val f = FrameLayout(this).apply {
            background = ViewUtils.resolveDrawable(this@MainActivity, android.R.attr.selectableItemBackgroundBorderless)
            setPadding(ViewUtils.dp(10), ViewUtils.dp(10), ViewUtils.dp(10), ViewUtils.dp(10))
        }
        val iv = ImageView(this).apply {
            setImageResource(iconRes)
            imageTintList = android.content.res.ColorStateList.valueOf(
                ViewUtils.resolveColor(this@MainActivity, android.R.attr.textColorPrimary)
            )
        }
        f.addView(iv, FrameLayout.LayoutParams(ViewUtils.dp(24), ViewUtils.dp(24), Gravity.CENTER))
        card.addView(f)
        return card
    }

    private fun buildBigCreamButton(iconRes: Int): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = ViewUtils.dp(32).toFloat()
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(ViewUtils.resolveColor(this@MainActivity, R.attr.colorPrimary))
        }
        val f = FrameLayout(this).apply {
            background = ViewUtils.resolveDrawable(this@MainActivity, android.R.attr.selectableItemBackgroundBorderless)
        }
        val iv = ImageView(this).apply {
            setImageResource(iconRes)
            imageTintList = android.content.res.ColorStateList.valueOf(
                ViewUtils.resolveColor(this@MainActivity, R.attr.colorOnPrimary)
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        f.addView(iv, FrameLayout.LayoutParams(ViewUtils.dp(54), ViewUtils.dp(54), Gravity.CENTER))
        card.addView(f, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return card
    }

    private fun buildSettingsHelpPages() {
        preferencesView = PreferencesCardView(this)
        homeView.addView(preferencesView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        helpView = HelpView(this)
        homeView.addView(helpView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun buildPermissionScreen(root: FrameLayout) {
        noPermsLayout = MaterialCardView(this@MainActivity).apply {
            setCardBackgroundColor(ViewUtils.resolveColor(this@MainActivity, R.attr.colorSurfaceContainerHigh))
            setStrokeColor(0)
            radius = ViewUtils.dp(32).toFloat()
        }
        val ll = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ViewUtils.dp(24), ViewUtils.dp(20), ViewUtils.dp(24), ViewUtils.dp(8))
        }
        val headerTitle = TextView(this).apply {
            setText(R.string.AppName)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(ViewUtils.resolveColor(this@MainActivity, android.R.attr.textColorPrimary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        }
        header.addView(headerTitle)
        ll.addView(header)

        batteryRow = PermissionRowView(this@MainActivity).apply {
            bind(R.string.BatteryOptimizationExclusion, PermissionsChecker.hasBatteryPerm(), true)
            setPadding(paddingLeft, ViewUtils.dp(6), paddingRight, paddingBottom)
            setOnClickListener {
                val r = it as PermissionRowView
                if (!r.isChecked) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                }
            }
        }
        ll.addView(batteryRow)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsRow = PermissionRowView(this@MainActivity).apply {
                bind(R.string.Notifications, PermissionsChecker.hasNotificationPerm(), true)
                setOnClickListener {
                    val r = it as PermissionRowView
                    if (!r.isChecked) {
                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
                    }
                }
            }
            ll.addView(notificationsRow)
        }
        if (PermissionsChecker.ENABLE_NOTIFICATIONS_CHANNEL_CHECK &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !PermissionsChecker.ignoreNotificationsChannel()
        ) {
            hideServicesChannelRow = PermissionRowView(this@MainActivity).apply {
                bind(R.string.HideNotificationsChannel, PermissionsChecker.isNotificationsChannelHidden(), true)
                setOnClickListener {
                    val r = it as PermissionRowView
                    if (!r.isChecked) {
                        Toast.makeText(this@MainActivity, getString(R.string.HideNotificationsChannelInfo, getString(R.string.ServicesChannel)), Toast.LENGTH_SHORT).show()
                        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, KlipperApp.SERVICES_CHANNEL))
                    }
                }
            }
            ll.addView(hideServicesChannelRow)
        }
        if (!PermissionsChecker.isNotBrokenBySDCard()) {
            brokenBySDCardRow = PermissionRowView(this@MainActivity).apply {
                bind(R.string.NotOnSdcard, PermissionsChecker.isNotBrokenBySDCard(), true)
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${KlipperApp.INSTANCE.packageName}")))
                    Toast.makeText(this@MainActivity, R.string.NotOnSdcardInfo, Toast.LENGTH_SHORT).show()
                }
            }
            ll.addView(brokenBySDCardRow)
        }

        val btnCard = MaterialCardView(this).apply {
            radius = ViewUtils.dp(24).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(ViewUtils.resolveColor(this@MainActivity, R.attr.colorPrimary))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)).apply {
                setMargins(ViewUtils.dp(16), ViewUtils.dp(12), ViewUtils.dp(16), ViewUtils.dp(20))
            }
        }
        val nextBtn = TextView(this@MainActivity).apply {
            setText(R.string.Next)
            setTextColor(ViewUtils.resolveColor(this@MainActivity, R.attr.colorOnPrimary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            background = ViewUtils.resolveDrawable(this@MainActivity, android.R.attr.selectableItemBackground)
            setOnClickListener {
                if (PermissionsChecker.needBlockStart()) return@setOnClickListener
                animateHomeView()
            }
        }
        btnCard.addView(nextBtn)
        ll.addView(btnCard)

        noPermsLayout.addView(ll)
        root.addView(noPermsLayout, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
            leftMargin = ViewUtils.dp(20)
            topMargin = ViewUtils.dp(20)
            rightMargin = ViewUtils.dp(20)
            bottomMargin = ViewUtils.dp(20)
        })

        noPermsLayout.visibility = if (PermissionsChecker.needBlockStart()) View.VISIBLE else View.GONE
        homeView.visibility = if (PermissionsChecker.needBlockStart()) View.GONE else View.VISIBLE
    }

    private fun refreshBottomButtons() {
        val hasInstances = instances.isNotEmpty()
        val anyRunning = instances.any { it.getState() == KlipperInstance.State.RUNNING }
        val anyStarting = instances.any { it.getState() == KlipperInstance.State.STARTING }
        val active = anyRunning || anyStarting

        addIcon.setImageResource(R.drawable.ic_add_outline_28)

        if (active) {
            runStopIcon.setImageResource(R.drawable.ic_stop_24)
        } else {
            runStopIcon.setImageResource(R.drawable.ic_play_28)
        }

        val addLp = addButton.layoutParams as LinearLayout.LayoutParams
        val runLp = runStopButton.layoutParams as LinearLayout.LayoutParams

        if (!hasInstances) {
            addButton.visibility = View.VISIBLE
            runStopButton.visibility = View.GONE
        } else {
            addButton.visibility = View.VISIBLE
            runStopButton.visibility = View.VISIBLE
        }
        addButton.requestLayout()
        runStopButton.requestLayout()
    }

    private fun runStopAll() {
        val hasInstances = instances.isNotEmpty()
        if (!hasInstances) return

        val anyRunningOrStarting = instances.any {
            it.getState() == KlipperInstance.State.RUNNING ||
            it.getState() == KlipperInstance.State.STARTING
        }

        if (anyRunningOrStarting) {
            for (inst in instances) {
                if (inst.getState() == KlipperInstance.State.RUNNING || inst.getState() == KlipperInstance.State.STARTING) {
                    if (inst.getState() != KlipperInstance.State.STOPPING) {
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
                    if (!KlipperInstance.hasFreeSlots()) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.NoFreeSlots)
                            .setMessage(getString(R.string.NoFreeSlotsDescription, KlipperInstance.SLOTS_COUNT))
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                        return
                    }
                    inst.start()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        KlipperApp.EVENT_BUS.unregisterListener(this)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = super.onKeyUp(keyCode, event)
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = super.onKeyDown(keyCode, event)

    fun isCurrentLauncher(): Boolean = isCurrentLauncher

    @EventHandler(runOnMainThread = true)
    fun onInstancesRefreshed(e: InstancesRefreshedEvent) {
        instances = ArrayList(KlipperInstance.getInstances())
        instancesAdapter.notifyDataSetChanged()
        refreshBottomButtons()
    }

    @EventHandler(runOnMainThread = true)
    fun onInstanceCreated(e: InstanceCreatedEvent) {
        instances = ArrayList(KlipperInstance.getInstances())
        instancesAdapter.notifyDataSetChanged()
        refreshBottomButtons()
    }

    @EventHandler(runOnMainThread = true)
    fun onInstanceUpdated(e: InstanceUpdatedEvent) {
        instances = ArrayList(KlipperInstance.getInstances())
        for (i in instances.indices) {
            if (instances[i].id == e.id) {
                instancesAdapter.notifyItemChanged(i + 1, NOTIFY_LIVE)
            }
        }
        refreshBottomButtons()
    }

    @EventHandler(runOnMainThread = true)
    fun onInstanceDestroyed(e: InstanceDestroyedEvent) {
        instances = ArrayList(KlipperInstance.getInstances())
        instancesAdapter.notifyDataSetChanged()
        refreshBottomButtons()
    }

    @EventHandler(runOnMainThread = true)
    fun onInstanceStateChanged(e: InstanceStateChangedEvent) {
        instances = ArrayList(KlipperInstance.getInstances())
        for (i in instances.indices) {
            if (instances[i].id == e.id) {
                instancesAdapter.notifyItemChanged(i + 1, NOTIFY_LIVE)
            }
        }
        refreshBottomButtons()
    }

    @EventHandler(runOnMainThread = true)
    fun onFrontendChanged(e: WebFrontendChangedEvent) {
        instancesAdapter.notifyItemChanged(0)
    }

    @EventHandler(runOnMainThread = true)
    fun onWebStateChanged(e: WebStateChangedEvent) {
        instancesAdapter.notifyItemChanged(0)
    }

    private fun invalidateHomeProgress(progress: Float) {
        val absP = Math.abs(progress)
        badgesLayout.alpha = 1f - absP
        instancesRecycler.alpha = 1f - absP * 0.7f
        badgesLayout.translationY = absP * ViewUtils.dp(16).toFloat()
        instancesRecycler.translationY = absP * ViewUtils.dp(30).toFloat()
        if (::bottomButtonsWrap.isInitialized) {
            bottomButtonsWrap.alpha = 1f - absP * 1.3f
            bottomButtonsWrap.translationY = absP * ViewUtils.dp(120).toFloat()
        }

        if (homeView.width > 0) {
            val w = homeView.width.toFloat()
            preferencesView.translationX = -w * (1f + Math.min(0f, progress))
            preferencesView.setProgress(-Math.min(0f, progress))

            helpView.translationX = w * (1f - Math.max(0f, progress))
            helpView.setProgress(Math.max(0f, progress))
        }

        if (progress <= -0.5f) {
            preferencesView.bringToFront()
        } else if (progress >= 0.5f) {
            helpView.bringToFront()
        } else {
            mainPage.bringToFront()
        }

        val mainInteractive = absP < 0.10f
        mainPage.isEnabled = mainInteractive
        mainPage.isClickable = mainInteractive
        preferencesView.isEnabled = progress < -0.10f
        preferencesView.isClickable = progress < -0.10f
        helpView.isEnabled = progress > 0.10f
        helpView.isClickable = progress > 0.10f
    }

    private fun animateHomeView() {
        SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1 / 256f)
            .setSpring(SpringForce(1f)
                .setStiffness(1000f)
                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
            .addUpdateListener { _, value, _ ->
                homeView.pivotX = homeView.width / 2f
                homeView.pivotY = homeView.height / 2f

                noPermsLayout.scaleX = ViewUtils.lerp(1f, 0.6f, value)
                noPermsLayout.scaleY = ViewUtils.lerp(1f, 0.6f, value)
                noPermsLayout.alpha = 1f - value

                homeView.scaleX = ViewUtils.lerp(0.6f, 1f, value)
                homeView.scaleY = ViewUtils.lerp(0.6f, 1f, value)
                homeView.alpha = value
            }
            .addEndListener { _, _, _, _ -> noPermsLayout.visibility = View.GONE }
            .also {
                homeView.visibility = View.VISIBLE
                it.start()
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_NOTIFICATIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    notificationsRow?.isChecked = true
                }
            }
            REQUEST_CAMERA -> {
                val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                preferencesView.refreshCameraSwitch(granted)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        batteryRow.isChecked = PermissionsChecker.hasBatteryPerm()
        hideServicesChannelRow?.isChecked = PermissionsChecker.isNotificationsChannelHidden()
        brokenBySDCardRow?.isChecked = PermissionsChecker.isNotBrokenBySDCard()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        if (intent != null && intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val prober = UsbSerialProber(KlipperProbeTable.getInstance())
            val manager = getSystemService(Context.USB_SERVICE) as UsbManager
            for (drv in prober.findAllDrivers(manager)) {
                if (!manager.hasPermission(drv.device)) {
                    manager.requestPermission(drv.device,
                        PendingIntent.getBroadcast(this, 0,
                            Intent(UsbSerialManager.ACTION_ON_DEVICE_CONNECTED).setPackage(packageName),
                            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_NO_CREATE))
                } else {
                    sendBroadcast(Intent(UsbSerialManager.ACTION_ON_DEVICE_CONNECTED)
                        .putExtra(UsbManager.EXTRA_DEVICE, drv.device)
                        .putExtra(UsbManager.EXTRA_PERMISSION_GRANTED, true)
                        .setPackage(packageName))
                }
            }
        }
    }
}
