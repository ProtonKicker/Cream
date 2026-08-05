package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class EditTextRowView(context: Context) : LinearLayout(context) {
    private val value: TextView
    private val title: TextView

    init {
        value = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
        }
        addView(value, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        title = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
        }
        addView(title, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        orientation = VERTICAL
        gravity = Gravity.CENTER
        background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
        setPadding(ViewUtils.dp(21), ViewUtils.dp(12), ViewUtils.dp(21), ViewUtils.dp(12))
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        minimumHeight = ViewUtils.dp(52)
    }

    val text: CharSequence?
        get() = if (title.visibility == GONE) null else value.text

    fun bind(title: Int, value: String?) {
        if (TextUtils.isEmpty(value)) {
            this.value.setText(title)
            this.title.visibility = GONE
        } else {
            this.title.setText(title)
            this.value.text = value
            this.title.visibility = VISIBLE
        }
    }
}
