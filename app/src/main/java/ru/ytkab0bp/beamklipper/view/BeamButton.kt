package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class BeamButton(context: Context) : AppCompatTextView(context) {
    private var colorRes = android.R.attr.colorAccent
    private var color = 0

    init {
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        setPadding(ViewUtils.dp(21), 0, ViewUtils.dp(21), 0)
        onApplyTheme()
    }

    fun setColor(color: Int) {
        this.color = color
        this.colorRes = 0
        onApplyTheme()
    }

    fun setColorRes(colorRes: Int) {
        this.colorRes = colorRes
        onApplyTheme()
    }

    fun onApplyTheme() {
        background = ViewUtils.createRipple(
            ViewUtils.resolveColor(context, android.R.attr.colorControlHighlight),
            if (colorRes != 0) ViewUtils.resolveColor(context, colorRes) else color, 16f)
        setTextColor(ViewUtils.resolveColor(context, R.attr.textColorOnAccent))
    }
}
