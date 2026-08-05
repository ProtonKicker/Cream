package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.materialswitch.MaterialSwitch
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class PermissionRowView(context: Context) : LinearLayout(context) {
    @JvmField
    var titleView: TextView
    @JvmField
    var mSwitch: MaterialSwitch

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var divider = false

    init {
        titleView = AppCompatTextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(titleView, LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
            marginEnd = ViewUtils.dp(12)
        })

        mSwitch = object : MaterialSwitch(context) {
            override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                return false
            }
        }.apply {
            trackTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    ViewUtils.resolveColor(context, android.R.attr.colorAccent),
                    0xFF7F7F7F.toInt()
                )
            )
            thumbTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    Color.WHITE,
                    0x44FFFFFF.toInt()
                )
            )
        }
        addView(mSwitch, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(ViewUtils.dp(21), ViewUtils.dp(6), ViewUtils.dp(16), ViewUtils.dp(6))
        orientation = HORIZONTAL
        setWillNotDraw(false)
        background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)

        dividerPaint.color = ViewUtils.resolveColor(context, R.attr.cardOutlineColor)
        dividerPaint.style = Paint.Style.STROKE
        dividerPaint.strokeWidth = ViewUtils.dp(1.5f).toFloat()
    }

    fun bind(text: Int, checked: Boolean, divider: Boolean) {
        titleView.setText(text)
        isChecked = checked
        this.divider = divider
        invalidate()
    }

    var isChecked: Boolean
        get() = mSwitch.isChecked
        set(value) {
            mSwitch.isChecked = value
            isEnabled = !value
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (divider) {
            canvas.drawLine(ViewUtils.dp(1.5f).toFloat(), (height - ViewUtils.dp(1)).toFloat(), (width - ViewUtils.dp(1.5f)).toFloat(), (height - ViewUtils.dp(1)).toFloat(), dividerPaint)
        }
    }
}
