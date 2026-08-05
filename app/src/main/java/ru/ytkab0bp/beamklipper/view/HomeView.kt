package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.content.res.Configuration
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.math.MathUtils
import androidx.core.util.Consumer
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

class HomeView(context: Context) : FrameLayout(context) {
    companion object {
        const val PAGE_SETTINGS = -1f
        const val PAGE_MAIN = 0f
        const val PAGE_HELP = 1f
        private const val SETTINGS_ENABLED = true
        private const val HELP_ENABLED = true
    }

    private var progressListener: Consumer<Float>? = null
    private val gestureDetector: GestureDetector
    private var touchSlop: Int
    private var startOffset = 0f
    private var startProgress = 0f
    private var isTouchDisabled = false
    private var processingSwipe = false
    private var animation: SpringAnimation? = null
    internal var progress = 0f
    private var scrollView: View? = null

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (!processingSwipe && !isTouchDisabled) {
                    val startX = e1?.x ?: e2.x
                    val endX = e2.x
                    val startY = e1?.y ?: e2.y
                    val endY = e2.y
                    val deltaX = endX - startX
                    val deltaY = endY - startY

                    val canScrollHorizontally = when {
                        progress == PAGE_MAIN && scrollView != null -> {
                            if (deltaX > 0) scrollView!!.canScrollHorizontally(-1)
                            else scrollView!!.canScrollHorizontally(1)
                        }
                        else -> false
                    }

                    if (Math.abs(deltaX) >= touchSlop && Math.abs(deltaX) >= Math.abs(deltaY) * 1.5f && !canScrollHorizontally) {
                        startOffset = deltaX
                        startProgress = progress
                        processingSwipe = true

                        val ev = MotionEvent.obtain(e2)
                        ev.action = MotionEvent.ACTION_CANCEL
                        for (i in 0 until childCount) {
                            getChildAt(i).dispatchTouchEvent(ev)
                        }
                        ev.recycle()
                    } else {
                        isTouchDisabled = true
                    }
                }
                if (processingSwipe) {
                    val deltaNorm = ((e2.x - (e1?.x ?: e2.x) - startOffset) / (width * 0.9f))
                    var target = startProgress - deltaNorm

                    if (!SETTINGS_ENABLED && target < 0f) target = 0f
                    if (!HELP_ENABLED && target > 0f) target = 0f

                    progress = MathUtils.clamp(target, -1f, 1f)
                    invalidateProgress()
                }
                return processingSwipe
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (processingSwipe && Math.abs(velocityX) >= 2500 && Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX > 0) {
                        when {
                            progress < -0.3f -> animateTo(PAGE_SETTINGS)
                            progress < 0.7f -> animateTo(PAGE_MAIN)
                            else -> animateTo(PAGE_HELP)
                        }
                    } else {
                        when {
                            progress > 0.3f -> animateTo(PAGE_HELP)
                            progress > -0.7f -> animateTo(PAGE_MAIN)
                            else -> animateTo(PAGE_SETTINGS)
                        }
                    }
                }
                return false
            }
        })
    }

    fun setScrollView(scrollView: View?) {
        this.scrollView = scrollView
    }

    fun animateTo(to: Float) = animateTo(to, null)

    fun animateTo(to: Float, callback: Runnable?) {
        if (progress == to) {
            callback?.run()
            return
        }
        animation = SpringAnimation(FloatValueHolder(progress))
            .setMinimumVisibleChange(1 / 256f)
            .setSpring(SpringForce(to)
                .setStiffness(700f)
                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
            .addUpdateListener { _, value, _ ->
                progress = value
                invalidateProgress()
            }
            .addEndListener { _, _, _, _ ->
                animation = null
                callback?.run()
            }
        animation?.start()
    }

    private fun invalidateProgress() {
        progressListener?.accept(progress)
    }

    fun getTargetProgress(): Float = animation?.spring?.finalPosition ?: progress

    fun getProgress(): Float = progress

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    fun setProgressListener(progressListener: Consumer<Float>?) {
        this.progressListener = progressListener
        progressListener?.accept(progress)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateProgress()
    }

    private fun clearFlags() {
        processingSwipe = false
        isTouchDisabled = false
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val det = gestureDetector.onTouchEvent(ev)
        if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
            if (processingSwipe) {
                if (animation == null && progress != PAGE_SETTINGS && progress != PAGE_MAIN && progress != PAGE_HELP) {
                    when {
                        progress < -0.5f -> animateTo(PAGE_SETTINGS)
                        progress > 0.5f -> animateTo(PAGE_HELP)
                        else -> animateTo(PAGE_MAIN)
                    }
                }
            }
            clearFlags()
        }
        return det || super.dispatchTouchEvent(ev) || ev.actionMasked == MotionEvent.ACTION_DOWN
    }
}
