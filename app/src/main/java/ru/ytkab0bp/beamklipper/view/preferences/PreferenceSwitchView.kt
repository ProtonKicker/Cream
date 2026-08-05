package ru.ytkab0bp.beamklipper.view.preferences

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class PreferenceSwitchView(context: Context) : LinearLayout(context) {
    private val title: TextView
    private val subtitle: TextView
    private val mSwitch: MaterialSwitch

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = ViewUtils.dp(52)
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(ViewUtils.dp(21), ViewUtils.dp(6), ViewUtils.dp(16), ViewUtils.dp(6))
        background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)

        val ll = LinearLayout(context).apply {
            orientation = VERTICAL
        }

        title = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
        }
        ll.addView(title)

        subtitle = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
        }
        ll.addView(subtitle)

        addView(ll, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        mSwitch = object : MaterialSwitch(context) {
            override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
        }
        addView(mSwitch, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    var isChecked: Boolean
        get() = mSwitch.isChecked
        set(value) { mSwitch.isChecked = value }

    fun bind(title: String, subtitle: String?, checked: Boolean) {
        this.title.text = title
        if (TextUtils.isEmpty(subtitle)) {
            this.subtitle.visibility = GONE
        } else {
            this.subtitle.text = subtitle
            this.subtitle.visibility = VISIBLE
        }
        mSwitch.isChecked = checked
        mSwitch.trackTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                ViewUtils.resolveColor(context, android.R.attr.colorAccent),
                0xFF7F7F7F.toInt()
            )
        )
        mSwitch.thumbTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(Color.WHITE, 0x44FFFFFF.toInt())
        )
    }
}
