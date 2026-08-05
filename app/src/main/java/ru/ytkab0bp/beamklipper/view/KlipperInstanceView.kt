package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.events.InstanceStateChangedEvent
import ru.ytkab0bp.beamklipper.events.WebStateChangedEvent
import ru.ytkab0bp.beamklipper.service.WebService
import ru.ytkab0bp.beamklipper.utils.Prefs
import ru.ytkab0bp.beamklipper.utils.ViewUtils
import ru.ytkab0bp.eventbus.EventHandler
import java.util.LinkedList

class KlipperInstanceView(context: Context) : LinearLayout(context) {
    private var id: String? = null
    private val cardView: CardView
    private val icon: ImageView
    private val titleSubtitleLayout: LinearLayout
    private val title: TextView
    private val subtitle: TextView
    private val startStopButton: StartStopButton
    private var visibleAnimation: SpringAnimation? = null
    private val visibleAnimationQueue = LinkedList<Runnable>()

    init {
        setPadding(ViewUtils.dp(16), ViewUtils.dp(12), ViewUtils.dp(16), ViewUtils.dp(12))
        gravity = Gravity.CENTER_VERTICAL
        setWillNotDraw(false)
        background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        cardView = CardView(context).apply {
            cardElevation = 0f
            radius = ViewUtils.dp(14).toFloat()
            val fl = FrameLayout(context).apply {
                setPadding(ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8))
                icon = ImageView(context).apply {
                    imageTintList = ColorStateList.valueOf(Color.WHITE)
                    layoutParams = LayoutParams(ViewUtils.dp(24), ViewUtils.dp(24))
                }
                addView(icon)
            }
            addView(fl)
        }
        addView(cardView)

        titleSubtitleLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            clipToPadding = false
            clipChildren = false
        }

        title = TextView(context).apply {
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        titleSubtitleLayout.addView(title)

        subtitle = TextView(context).apply {
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            visibility = GONE
        }
        titleSubtitleLayout.addView(subtitle)

        addView(titleSubtitleLayout, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = ViewUtils.dp(12)
            rightMargin = ViewUtils.dp(12)
        })

        startStopButton = StartStopButton(context).apply {
            setPadding(ViewUtils.dp(7), ViewUtils.dp(7), ViewUtils.dp(7), ViewUtils.dp(7))
            layoutParams = LayoutParams(ViewUtils.dp(34), ViewUtils.dp(34))
        }
        addView(startStopButton)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        KlipperApp.EVENT_BUS.registerListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        KlipperApp.EVENT_BUS.unregisterListener(this)
    }

    fun setColorIndex(i: Int) {
        val idx = Math.abs(i) % 10
        cardView.setCardBackgroundColor(ViewUtils.resolveColor(context,
            when (idx) {
                1 -> R.attr.startStopButtonColor_1
                2 -> R.attr.startStopButtonColor_2
                3 -> R.attr.startStopButtonColor_3
                4 -> R.attr.startStopButtonColor_4
                5 -> R.attr.startStopButtonColor_5
                6 -> R.attr.startStopButtonColor_6
                7 -> R.attr.startStopButtonColor_7
                8 -> R.attr.startStopButtonColor_8
                9 -> R.attr.startStopButtonColor_9
                else -> R.attr.startStopButtonColor_0
            }))
        invalidate()
    }

    fun bindWeb() {
        id = null
        when (Prefs.webFrontend) {
            Prefs.FRONTEND_FLUIDD -> {
                icon.setImageResource(R.drawable.ic_square_stack_up_outline_28)
                title.setText(R.string.Fluidd)
                setColorIndex(9)
            }
            else -> {
                icon.setImageResource(R.drawable.ic_sailing_24)
                title.setText(R.string.Mainsail)
                setColorIndex(6)
            }
        }

        val visible = KlipperInstance.isWebServerRunning()
        subtitle.visibility = if (visible) VISIBLE else GONE
        subtitle.tag = visible
        if (visible) {
            bindWebSubtitle()
        }
        setOnClickListener {
            val wm = KlipperApp.INSTANCE.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val i = wm.connectionInfo.ipAddress
            val ip = if (i == 0 || !KlipperInstance.isWebServerRunning()) "127.0.0.1" else Formatter.formatIpAddress(i)
            val t = System.currentTimeMillis()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$ip:${WebService.PORT}/?t=$t"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            it.context.startActivity(intent)
        }
        isClickable = visible
        startStopButton.visibility = GONE
    }

    private fun bindWebSubtitle() {
        val wm = KlipperApp.INSTANCE.getSystemService(Context.WIFI_SERVICE) as WifiManager
        subtitle.text = KlipperApp.INSTANCE.getString(R.string.IPInfo, Formatter.formatIpAddress(wm.connectionInfo.ipAddress), WebService.PORT)
    }

    fun bind(instance: KlipperInstance) {
        id = instance.id
        icon.setImageResource(instance.icon.drawable)
        title.text = instance.name

        if (instance.getState() == KlipperInstance.State.STARTING) {
            subtitle.setText(R.string.InstanceStarting)
        } else if (instance.getState() == KlipperInstance.State.STOPPING) {
            subtitle.setText(R.string.InstanceStopping)
        }

        val wasVisible = subtitle.tag != null
        val visible = instance.getState() == KlipperInstance.State.STARTING || instance.getState() == KlipperInstance.State.STOPPING
        if (visible != wasVisible) {
            subtitle.visibility = if (visible) VISIBLE else GONE
            subtitle.tag = if (visible) true else null
        }

        setColorIndex(id.hashCode() % 10)
        startStopButton.setColorIndex(id.hashCode() % 10)
        startStopButton.setStopped(instance.getState() != KlipperInstance.State.RUNNING && instance.getState() != KlipperInstance.State.STOPPING)
        startStopButton.setOnClickListener {
            val inst = KlipperInstance.getInstance(id ?: return@setOnClickListener) ?: return@setOnClickListener
            if (inst.getState() == KlipperInstance.State.STARTING || inst.getState() == KlipperInstance.State.STOPPING) return@setOnClickListener

            if (inst.getState() == KlipperInstance.State.IDLE) {
                if (!KlipperInstance.hasFreeSlots()) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.NoFreeSlots)
                        .setMessage(context.getString(R.string.NoFreeSlotsDescription, KlipperInstance.SLOTS_COUNT))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    return@setOnClickListener
                }
                inst.start()
            } else {
                inst.stop()
                if (inst.autostart) {
                    inst.autostart = false
                    KlipperApp.DATABASE.update(inst)
                }
            }
        }
        invalidate()
    }

    private fun animateSubtitle(visible: Boolean) {
        subtitle.tag = if (visible) true else null
        if (visibleAnimation != null) {
            visibleAnimationQueue.push(Runnable { animateSubtitle(visible) })
            return
        }

        val fY: Float
        val tY: Float
        if (visible) {
            subtitle.visibility = VISIBLE
            subtitle.alpha = 0f
            fY = ViewUtils.dp(8).toFloat()
            tY = 0f
        } else {
            fY = 0f
            tY = ViewUtils.dp(8).toFloat()
        }
        visibleAnimation = SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1 / 256f)
            .setSpring(SpringForce(1f).setStiffness(1000f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
            .addUpdateListener { _, value, _ ->
                titleSubtitleLayout.translationY = ViewUtils.lerp(fY, tY, value)
                subtitle.alpha = if (visible) value else 1f - value
            }
            .addEndListener { _, canceled, _, _ ->
                if (!canceled) {
                    if (!visible) {
                        subtitle.visibility = GONE
                        titleSubtitleLayout.translationY = 0f
                    }
                }
                visibleAnimation = null
                if (visibleAnimationQueue.isNotEmpty()) {
                    visibleAnimationQueue.removeAt(0).run()
                }
            }
        titleSubtitleLayout.translationY = fY
        visibleAnimation?.start()
    }

    @EventHandler(runOnMainThread = true)
    fun onWebStateChanged(e: WebStateChangedEvent) {
        if (id == null) {
            if (e.state == KlipperInstance.State.RUNNING) {
                bindWebSubtitle()
            }
            val wasVisible = subtitle.tag != null
            val visible = e.state == KlipperInstance.State.RUNNING
            if (visible != wasVisible) {
                animateSubtitle(visible)
                isClickable = visible
            }
        }
    }

    @EventHandler(runOnMainThread = true)
    fun onStateChanged(e: InstanceStateChangedEvent) {
        if (id == e.id) {
            if (e.state == KlipperInstance.State.STARTING) {
                subtitle.setText(R.string.InstanceStarting)
            } else if (e.state == KlipperInstance.State.STOPPING) {
                subtitle.setText(R.string.InstanceStopping)
            }

            val wasVisible = subtitle.tag != null
            val visible = e.state == KlipperInstance.State.STARTING || e.state == KlipperInstance.State.STOPPING
            if (visible != wasVisible) {
                animateSubtitle(visible)
            }

            startStopButton.setStopped(e.state != KlipperInstance.State.RUNNING && e.state != KlipperInstance.State.STOPPING)
        }
    }
}
