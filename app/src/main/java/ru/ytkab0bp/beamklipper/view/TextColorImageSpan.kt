package ru.ytkab0bp.beamklipper.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan

class TextColorImageSpan(drawable: Drawable, private val offsetY: Float) : ImageSpan(drawable, ALIGN_BASELINE) {
    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        drawable.setTint(paint.color)
        canvas.save()
        canvas.translate(0f, offsetY)
        super.draw(canvas, text, start, end, x, top, y, bottom, paint)
        canvas.restore()
    }
}
