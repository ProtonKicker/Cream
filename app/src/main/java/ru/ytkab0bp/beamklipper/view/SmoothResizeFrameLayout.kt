package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import java.util.ArrayList

class SmoothResizeFrameLayout : FrameLayout {
    private val mMatchParentChildren = ArrayList<View>(1)
    private var widthValue: FloatValueHolder? = null
    private var mWidthSpring: SpringAnimation? = null
    private var heightValue: FloatValueHolder? = null
    private var mHeightSpring: SpringAnimation? = null
    private var ignoreNextLayout = false
    private val forceNotMeasure = ArrayList<View>()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mWidthSpring = SpringAnimation(widthValue.let { FloatValueHolder(width.toFloat()) }.also { widthValue = it })
            .setSpring(SpringForce(width.toFloat()).setStiffness(1000f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
            .addUpdateListener { _, _, _ -> invalidateSize() }
        mHeightSpring = SpringAnimation(heightValue.let { FloatValueHolder(height.toFloat()) }.also { heightValue = it })
            .setSpring(SpringForce(height.toFloat()).setStiffness(1000f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
            .addUpdateListener { _, _, _ -> invalidateSize() }
    }

    private fun invalidateSize() {
        val wv = widthValue ?: return
        val hv = heightValue ?: return
        val w = wv.value.toInt()
        val h = hv.value.toInt()
        if (measuredWidth == w && measuredHeight == h) return
        setMeasuredDimension(w, h)
        requestLayout()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mWidthSpring?.cancel()
        mWidthSpring = null
        mHeightSpring?.cancel()
        mHeightSpring = null
    }

    fun ignoreNextLayout() {
        ignoreNextLayout = true
    }

    fun addForceNotMeasure(v: View) {
        forceNotMeasure.add(v)
    }

    fun removeForceNotMeasure(v: View) {
        forceNotMeasure.remove(v)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val count = childCount
        val measureMatchParentChildren =
            MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                    MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
        mMatchParentChildren.clear()

        var maxHeight = 0
        var maxWidth = 0
        var childState = 0

        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE && !forceNotMeasure.contains(child)) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                val lp = child.layoutParams as LayoutParams
                maxWidth = maxOf(maxWidth, child.measuredWidth + lp.leftMargin + lp.rightMargin)
                maxHeight = maxOf(maxHeight, child.measuredHeight + lp.topMargin + lp.bottomMargin)
                childState = combineMeasuredStates(childState, child.measuredState)
                if (measureMatchParentChildren) {
                    if (lp.width == LayoutParams.MATCH_PARENT || lp.height == LayoutParams.MATCH_PARENT) {
                        mMatchParentChildren.add(child)
                    }
                }
            }
        }

        maxWidth += paddingLeft + paddingRight
        maxHeight += paddingTop + paddingBottom
        maxHeight = maxOf(maxHeight, suggestedMinimumHeight)
        maxWidth = maxOf(maxWidth, suggestedMinimumWidth)
        val drawable = foreground
        if (drawable != null) {
            maxHeight = maxOf(maxHeight, drawable.minimumHeight)
            maxWidth = maxOf(maxWidth, drawable.minimumWidth)
        }

        val mmCount = mMatchParentChildren.size
        if (mmCount > 1) {
            for (i in 0 until mmCount) {
                val child = mMatchParentChildren[i]
                val lp = child.layoutParams as MarginLayoutParams

                val childWidthMeasureSpec: Int
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    val w = maxOf(0, measuredWidth - paddingLeft - paddingRight - lp.leftMargin - lp.rightMargin)
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY)
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                        paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin, lp.width)
                }

                val childHeightMeasureSpec: Int
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    val h = maxOf(0, measuredHeight - paddingTop - paddingBottom - lp.topMargin - lp.bottomMargin)
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                        paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin, lp.height)
                }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            }
        }

        val measuredWidth = MeasureSpec.getSize(resolveSizeAndState(maxWidth, widthMeasureSpec, childState))
        val measuredHeight = MeasureSpec.getSize(resolveSizeAndState(maxHeight, heightMeasureSpec, childState shl MEASURED_HEIGHT_STATE_SHIFT))

        if (ignoreNextLayout && isLaidOut) {
            ignoreNextLayout = false
            mWidthSpring?.spring?.finalPosition = measuredWidth.toFloat()
            mWidthSpring?.start()
            mHeightSpring?.spring?.finalPosition = measuredHeight.toFloat()
            mHeightSpring?.start()
            return
        }

        val ws = mWidthSpring ?: return
        val hs = mHeightSpring ?: return
        if (ws.spring.finalPosition != 0f) {
            ws.spring.finalPosition = measuredWidth.toFloat()
            ws.start()
        } else {
            ws.cancel()
            ws.spring.finalPosition = measuredWidth.toFloat()
            (widthValue ?: return).value = measuredWidth.toFloat()
        }
        if (hs.spring.finalPosition != 0f) {
            hs.spring.finalPosition = measuredHeight.toFloat()
            hs.start()
        } else {
            hs.cancel()
            hs.spring.finalPosition = measuredHeight.toFloat()
            (heightValue ?: return).value = measuredHeight.toFloat()
        }
        setMeasuredDimension((widthValue?.value ?: return).toInt(), (heightValue?.value ?: return).toInt())
    }
}
