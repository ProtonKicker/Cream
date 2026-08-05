package ru.ytkab0bp.beamklipper.view.preferences

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class PreferenceHeaderView(context: Context) : AppCompatTextView(context) {
    init {
        setPadding(ViewUtils.dp(21), ViewUtils.dp(6), ViewUtils.dp(21), 0)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        setTextColor(ViewUtils.resolveColor(context, android.R.attr.colorAccent))
    }
}
