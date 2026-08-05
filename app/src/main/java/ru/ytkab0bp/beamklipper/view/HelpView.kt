package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Region
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import ru.ytkab0bp.beamklipper.BuildConfig
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.MainActivity
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class HelpView(context: Context) : FrameLayout(context) {
    companion object {
        private const val MIN_HEIGHT_DP = 64
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dimmPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var progress = 0f
    internal val header: LinearLayout
    private val title: TextView

    init {
        dimmPaint.color = Color.BLACK
        outlinePaint.style = Paint.Style.FILL
        outlinePaint.color = ViewUtils.resolveColor(context, R.attr.cardOutlineColor)
        bgPaint.color = ViewUtils.resolveColor(context, android.R.attr.windowBackground)

        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        header = LinearLayout(context).apply {
            setPadding(ViewUtils.dp(21), ViewUtils.dp(8), ViewUtils.dp(21), 0)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        title = TextView(context).apply {
            setText(R.string.HelpTitle)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            gravity = Gravity.CENTER
        }
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        ll.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(48)))
        header.setOnApplyWindowInsetsListener { v, insets ->
            val topInset = insets.systemWindowInsetTop
            v.setPadding(ViewUtils.dp(21), ViewUtils.dp(8) + topInset, ViewUtils.dp(21), 0)
            val lp = v.layoutParams as LinearLayout.LayoutParams
            lp.height = ViewUtils.dp(48) + topInset
            v.layoutParams = lp
            insets
        }

        val scrollView = androidx.core.widget.NestedScrollView(context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewUtils.dp(24), ViewUtils.dp(8), ViewUtils.dp(24), ViewUtils.dp(40))
        }

        val appNameTv = TextView(context).apply {
            setText(R.string.AppName)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        }
        content.addView(appNameTv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(4)
        })

        val versionTv = TextView(context).apply {
            text = "v${BuildConfig.VERSION_NAME} · ${BuildConfig.COMMIT}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
        }
        content.addView(versionTv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(28)
        })

        val quickStartTitle = TextView(context).apply {
            setText(R.string.IntroTitle)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        }
        content.addView(quickStartTitle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(12)
        })

        val introTv = TextView(context).apply {
            setText(R.string.IntroText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            setLineSpacing(ViewUtils.dp(4).toFloat(), 1f)
        }
        content.addView(introTv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(28)
        })

        val privacyTv = TextView(context).apply {
            setText(R.string.PrivacyNote)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
            setPadding(ViewUtils.dp(16), ViewUtils.dp(12), ViewUtils.dp(16), ViewUtils.dp(12))
            setBackgroundResource(R.color.surfaceCreamContainer)
        }
        (privacyTv.background as? android.graphics.drawable.GradientDrawable)?.cornerRadius = ViewUtils.dp(16).toFloat()
        content.addView(privacyTv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(32)
        })

        val infoTitle = TextView(context).apply {
            text = "Information"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        }
        content.addView(infoTitle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(4)
        })

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ViewUtils.resolveColor(context, R.attr.dividerColor)
            style = Paint.Style.STROKE
            strokeWidth = ViewUtils.dp(0.5f).toFloat()
        }
        addRow(content, R.string.GitHubRepo, R.drawable.ic_github_28, false) {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/utkabobr/BeamKlipper"))
            context.startActivity(i)
        }

        scrollView.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        ll.addView(scrollView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        addView(ll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        setWillNotDraw(false)
    }

    private fun addRow(container: LinearLayout, titleRes: Int, iconRes: Int, divider: Boolean, onClick: (() -> Unit)?) {
        val row = FrameLayout(context).apply {
            setPadding(ViewUtils.dp(8), ViewUtils.dp(14), ViewUtils.dp(8), ViewUtils.dp(14))
            if (onClick != null) {
                background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
                setOnClickListener { onClick() }
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(context).apply {
            setImageResource(iconRes)
            imageTintList = android.content.res.ColorStateList.valueOf(
                ViewUtils.resolveColor(context, android.R.attr.textColorPrimary)
            )
            layoutParams = LinearLayout.LayoutParams(ViewUtils.dp(24), ViewUtils.dp(24)).apply {
                marginEnd = ViewUtils.dp(16)
            }
        }
        inner.addView(icon)
        val label = TextView(context).apply {
            setText(titleRes)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        inner.addView(label)
        if (onClick != null) {
            val chevron = ImageView(context).apply {
                setImageResource(R.drawable.ic_chevron_up_outline_28)
                rotation = 90f
                imageTintList = android.content.res.ColorStateList.valueOf(
                    ViewUtils.resolveColor(context, android.R.attr.textColorSecondary)
                )
                layoutParams = LinearLayout.LayoutParams(ViewUtils.dp(20), ViewUtils.dp(20))
            }
            inner.addView(chevron)
        }
        row.addView(inner, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = ViewUtils.dp(8)
            rightMargin = ViewUtils.dp(8)
        })
        container.addView(row)
        if (divider) {
            val div = View(context).apply {
                setBackgroundColor(ViewUtils.resolveColor(context, R.attr.dividerColor))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(0.5f)).apply {
                    leftMargin = ViewUtils.dp(48)
                    rightMargin = ViewUtils.dp(16)
                }
            }
            container.addView(div)
        }
    }

    override fun draw(canvas: Canvas) {
        val p = Math.max(0f, Math.min(1f, progress))
        val radius = (1f - p) * ViewUtils.dp(32)
        path.rewind()
        path.addRoundRect(
            0f,
            0f,
            width.toFloat(),
            height + radius,
            radius, radius,
            Path.Direction.CW
        )
        canvas.save()
        canvas.clipPath(path)
        canvas.drawPaint(bgPaint)
        super.draw(canvas)
        canvas.restore()
    }

    private fun invalidateProgress() {
        val p = Math.max(0f, Math.min(1f, progress))
        alpha = p

        if (context is MainActivity) {
            val w: android.view.Window = (context as MainActivity).window
            w.navigationBarColor = ColorUtils.blendARGB(
                ViewUtils.resolveColor(context, R.attr.navbarColor),
                ViewUtils.resolveColor(context, android.R.attr.windowBackground),
                p
            )
        }
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidateProgress()
        invalidate()
    }
}
