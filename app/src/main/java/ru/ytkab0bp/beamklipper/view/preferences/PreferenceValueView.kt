package ru.ytkab0bp.beamklipper.view.preferences

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class PreferenceValueView(context: Context) : LinearLayout(context) {
    private val title: TextView
    private val value: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
        setPadding(ViewUtils.dp(21), ViewUtils.dp(16), ViewUtils.dp(16), ViewUtils.dp(16))
        minimumHeight = ViewUtils.dp(52)
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        title = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
        }
        addView(title, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        value = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.colorAccent))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        }
        addView(value, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginStart = ViewUtils.dp(8)
        })
    }

    fun bind(title: String, value: String) {
        this.title.text = title
        this.value.text = value
    }
}
