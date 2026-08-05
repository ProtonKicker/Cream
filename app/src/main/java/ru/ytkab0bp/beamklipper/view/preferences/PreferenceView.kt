package ru.ytkab0bp.beamklipper.view.preferences

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class PreferenceView(context: Context) : LinearLayout(context) {
    private val title: TextView
    private val subtitle: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
        setPadding(ViewUtils.dp(21), ViewUtils.dp(16), ViewUtils.dp(16), ViewUtils.dp(16))
        minimumHeight = ViewUtils.dp(52)
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        title = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
        }
        addView(title)

        subtitle = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
        }
        addView(subtitle)
    }

    fun bind(title: String, subtitle: String?) {
        this.title.text = title
        if (TextUtils.isEmpty(subtitle)) {
            this.subtitle.visibility = GONE
        } else {
            this.subtitle.text = subtitle
            this.subtitle.visibility = VISIBLE
        }
    }
}
