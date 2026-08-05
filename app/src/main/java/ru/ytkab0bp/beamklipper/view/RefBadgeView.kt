package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class RefBadgeView(context: Context) : LinearLayout(context) {
    val icon: ImageView
    private val title: TextView
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var outlinePaint: Paint? = null
    private val path = Path()
    private var progress = 0f

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        icon = ImageView(context).apply {
            setColorFilter(Color.WHITE)
            layoutParams = LayoutParams(ViewUtils.dp(22 + 18), ViewUtils.dp(22 + 18))
            setPadding(ViewUtils.dp(9), ViewUtils.dp(9), ViewUtils.dp(9), ViewUtils.dp(9))
        }
        addView(icon)

        title = TextView(context).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        }
        addView(title, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = ViewUtils.dp(8)
            marginEnd = ViewUtils.dp(22 + 9)
        })

        setWillNotDraw(false)
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(22 + 18)).apply {
            leftMargin = ViewUtils.dp(9)
            rightMargin = ViewUtils.dp(9)
        }
    }

    override fun draw(canvas: Canvas) {
        path.rewind()
        path.addRoundRect(
            0f, 0f,
            ViewUtils.lerp(height.toFloat(), width.toFloat(), progress), height.toFloat(),
            height / 2f, height / 2f,
            Path.Direction.CW
        )
        canvas.save()
        canvas.clipPath(path)

        if (outlinePaint == null) {
            canvas.drawPaint(paint)
        }

        super.draw(canvas)
        canvas.restore()

        outlinePaint?.let { op ->
            val stroke = op.strokeWidth
            canvas.drawRoundRect(
                stroke, stroke,
                ViewUtils.lerp(height.toFloat(), width.toFloat(), progress) - stroke,
                height - stroke,
                height / 2f, height / 2f,
                op
            )
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if ((ev.actionMasked == MotionEvent.ACTION_DOWN || ev.actionMasked == MotionEvent.ACTION_MOVE) &&
            ev.x > ViewUtils.lerp(height.toFloat(), width.toFloat(), progress)
        ) {
            return false
        }
        return super.dispatchTouchEvent(ev)
    }

    fun setColored() {
        icon.setColorFilter(null)
        outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ViewUtils.dp(1.5f).toFloat()
            color = ViewUtils.resolveColor(context, R.attr.dividerColor)
        }
        title.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
    }

    fun setIcon(@DrawableRes i: Int, @AttrRes bgColor: Int, @StringRes titleRes: Int) {
        icon.setImageResource(i)
        if (bgColor != 0) {
            paint.color = ViewUtils.resolveColor(context, bgColor)
            background = ViewUtils.createRipple(0x21000000, 0f)
        } else {
            setColored()
            background = ViewUtils.createRipple(ViewUtils.resolveColor(context, android.R.attr.colorControlHighlight), 0f)
        }
        title.setText(titleRes)
    }

    fun setProgress(v: Float) {
        progress = v
        title.alpha = v
        invalidate()
    }
}
