package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.graphics.*
import androidx.core.math.MathUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class FadeRecyclerView(context: Context) : RecyclerView(context) {
    companion object {
        private const val HEIGHT_DP = 32
    }

    private val topPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var topProgress = 0f
    private var bottomProgress = 0f
    private var overlayAlpha = 1f
    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null
    private var bitmapMode = false

    init {
        val llm = LinearLayoutManager(context)
        setLayoutManager(llm)
        setWillNotDraw(false)
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                topProgress = 1f
                if (llm.findFirstVisibleItemPosition() == 0) {
                    val ch = llm.getChildAt(0) ?: return
                    val size = minOf(ch.height, ViewUtils.dp(HEIGHT_DP) / 2)
                    topProgress = MathUtils.clamp(-ch.top / size.toFloat(), 0f, 1f)
                }
                bottomProgress = 1f
                if (llm.findLastVisibleItemPosition() == (recyclerView.adapter?.itemCount ?: return) - 1) {
                    val ch = llm.getChildAt(llm.childCount - 1) ?: return
                    val size = minOf(ch.height, ViewUtils.dp(HEIGHT_DP) / 2)
                    bottomProgress = MathUtils.clamp((ch.bottom - height) / size.toFloat(), 0f, 1f)
                }
                invalidate()
            }
        })
        invalidateShaders()
    }

    fun setBitmapMode() {
        bitmapMode = true
        topPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        bottomPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        invalidateShaders()
    }

    fun setOverlayAlpha(overlayAlpha: Float) {
        this.overlayAlpha = overlayAlpha
        invalidate()
    }

    override fun draw(c: Canvas) {
        val cv: Canvas
        if (bitmapMode) {
            if (bitmap == null || bitmap!!.width != width || bitmap!!.height != height) {
                bitmap?.recycle()
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmapCanvas = Canvas(bitmap!!)
            }
            bitmap!!.eraseColor(Color.TRANSPARENT)
            cv = bitmapCanvas!!
            super.draw(cv)
        } else {
            cv = c
            super.draw(cv)
        }

        if (topProgress > 0) {
            cv.save()
            if (bitmapMode) {
                cv.translate(0f, -ViewUtils.dp(HEIGHT_DP) * (1f - topProgress * overlayAlpha))
            } else {
                topPaint.alpha = (topProgress * overlayAlpha * 0xFF).toInt()
            }
            cv.drawRect(0f, 0f, width.toFloat(), ViewUtils.dp(HEIGHT_DP).toFloat(), topPaint)
            cv.restore()
        }
        if (bottomProgress > 0) {
            cv.save()
            if (bitmapMode) {
                cv.translate(0f, ViewUtils.dp(HEIGHT_DP) * (1f - bottomProgress * overlayAlpha))
            } else {
                bottomPaint.alpha = (bottomProgress * overlayAlpha * 0xFF).toInt()
            }
            cv.drawRect(0f, (height - ViewUtils.dp(HEIGHT_DP)).toFloat(), width.toFloat(), height.toFloat(), bottomPaint)
            cv.restore()
        }

        if (bitmapMode) {
            bitmap?.let { c.drawBitmap(it, 0f, 0f, null) }
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bitmap?.recycle()
        bitmap = null
        bitmapCanvas = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateShaders()
    }

    private fun invalidateShaders() {
        if (width == 0 || height == 0) return
        val clr = if (bitmapMode) Color.BLACK else ViewUtils.resolveColor(context, android.R.attr.windowBackground)
        topPaint.shader = LinearGradient(
            width / 2f, 0f,
            width / 2f, ViewUtils.dp(HEIGHT_DP).toFloat(),
            if (bitmapMode) 0 else clr,
            if (bitmapMode) clr else 0,
            Shader.TileMode.CLAMP)
        bottomPaint.shader = LinearGradient(
            width / 2f, (height - ViewUtils.dp(HEIGHT_DP)).toFloat(),
            width / 2f, height.toFloat(),
            if (bitmapMode) clr else 0,
            if (bitmapMode) 0 else clr,
            Shader.TileMode.CLAMP)
        invalidate()
    }
}
