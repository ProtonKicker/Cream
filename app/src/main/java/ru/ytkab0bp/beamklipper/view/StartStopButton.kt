package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class StartStopButton : AppCompatImageView {
    companion object {
        private const val DEFAULT_RADIUS = 30
        private const val MIN_RADIUS = 14
    }

    private val backgroundPaint = Paint()
    private val path = Path()
    private var progress = 0f
    private var wasStopped = true
    private var spring: SpringAnimation? = null
    private var mDrawable: Drawable? = null
    private var mFilter: ColorFilter? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setImageResource(R.drawable.ic_play_28)
        setWillNotDraw(false)
        background = ViewUtils.createRipple(
            ViewUtils.resolveColor(context, android.R.attr.colorControlHighlight), MIN_RADIUS.toFloat())
        colorFilter = PorterDuffColorFilter(
            ViewUtils.resolveColor(context, R.attr.startStopButtonForegroundColor), PorterDuff.Mode.SRC_IN)
        invalidate()
    }

    fun setColorIndex(i: Int) {
        val idx = Math.abs(i) % 10
        backgroundPaint.color = ViewUtils.resolveColor(context,
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
            })
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        path.rewind()
        val rad = ViewUtils.dp(ViewUtils.lerp(DEFAULT_RADIUS.toFloat(), MIN_RADIUS.toFloat(), progress)).toFloat()
        path.addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), rad, rad, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        canvas.drawPaint(backgroundPaint)
        canvas.save()
        val sc = if (progress < 0.5f) 1f - progress else progress
        canvas.scale(sc, sc, width / 2f, height / 2f)
        mDrawable?.bounds = Rect(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)
        mDrawable?.draw(canvas)
        canvas.restore()
        super.draw(canvas)
        canvas.restore()
    }

    override fun setImageResource(resId: Int) {
        mDrawable = ContextCompat.getDrawable(context, resId)
        mDrawable?.colorFilter = mFilter
        invalidate()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mFilter = cf
        mDrawable?.colorFilter = cf
    }

    fun setStopped(stopped: Boolean) {
        if (wasStopped == stopped) return
        val current = if (wasStopped) 0f else 1f
        wasStopped = stopped
        spring?.cancel()
        spring = SpringAnimation(FloatValueHolder(current))
            .setMinimumVisibleChange(1 / 256f)
            .setSpring(SpringForce(if (stopped) 0f else 1f)
                .setStiffness(900f)
                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
            .addUpdateListener { _, value, _ ->
                if (stopped && value < 0.5f || !stopped && value > 0.5f) {
                    setImageResource(if (stopped) R.drawable.ic_play_28 else R.drawable.ic_stop_24)
                }
                setProgress(value)
            }
            .addEndListener { _, _, _, _ -> spring = null }
        spring?.start()
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }
}
