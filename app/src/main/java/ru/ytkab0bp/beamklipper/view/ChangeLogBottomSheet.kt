package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.utils.ViewUtils
import java.nio.charset.StandardCharsets
import java.util.Locale

class ChangeLogBottomSheet(context: Context) : BottomSheetDialog(context) {
    init {
        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val gd = GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    ViewUtils.dp(28).toFloat(), ViewUtils.dp(28).toFloat(),
                    ViewUtils.dp(28).toFloat(), ViewUtils.dp(28).toFloat(),
                    0f, 0f,
                    0f, 0f
                )
                setColor(ViewUtils.resolveColor(context, android.R.attr.windowBackground))
            }
            background = gd
            setPadding(0, ViewUtils.dp(12), 0, ViewUtils.dp(12))
        }

        ll.addView(TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            setText(R.string.Changelog)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = ViewUtils.dp(21)
            rightMargin = ViewUtils.dp(21)
        })

        val scrollView = ScrollView(context)
        val text = TextView(context).apply {
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setPadding(ViewUtils.dp(16), ViewUtils.dp(12), ViewUtils.dp(16), ViewUtils.dp(12))
        }
        scrollView.addView(text)

        try {
            context.assets.open("update.json").use { inp ->
                val obj = JSONObject(inp.readBytes().toString(StandardCharsets.UTF_8))
                val code = Locale.getDefault().language
                text.text = if (obj.has(code)) obj.getString(code) else obj.getString("en")
            }
        } catch (e: Exception) {
            Log.e("Changelog", "Failed to open update file", e)
        }

        ll.addView(scrollView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(320)))

        val btn = BeamButton(context).apply {
            setText(R.string.ChangelogOK)
            setOnClickListener { dismiss() }
        }
        ll.addView(btn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(48)).apply {
            leftMargin = ViewUtils.dp(12)
            topMargin = ViewUtils.dp(12)
            rightMargin = ViewUtils.dp(12)
            bottomMargin = ViewUtils.dp(12)
        })

        ll.fitsSystemWindows = true
        setContentView(ll)
    }

    override fun show() {
        super.show()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}
