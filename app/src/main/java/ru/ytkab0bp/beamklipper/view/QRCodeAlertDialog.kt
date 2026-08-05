package ru.ytkab0bp.beamklipper.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.annotation.NonNull
import androidx.cardview.widget.CardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.utils.ViewUtils

class QRCodeAlertDialog(@NonNull context: Context, link: String) : MaterialAlertDialogBuilder(context) {
    init {
        setTitle(R.string.QRCode)
        setPositiveButton(R.string.QROpen) { _, _ ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }
        setNegativeButton(R.string.QRCancel, null)
        setNeutralButton(R.string.QRCopy) { _, _ ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("", link)
            clipboard.setPrimaryClip(clip)
        }

        val outer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewUtils.dp(0), ViewUtils.dp(4), ViewUtils.dp(0), ViewUtils.dp(4))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val qrWrap = FrameLayout(context)
        val iv = ImageView(context)
        val encoder = QRGEncoder(link, null, QRGContents.Type.TEXT, ViewUtils.dp(250))
        encoder.setColorWhite(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
        encoder.setColorBlack(0)
        iv.setImageBitmap(encoder.bitmap)
        qrWrap.addView(iv, FrameLayout.LayoutParams(ViewUtils.dp(250), ViewUtils.dp(250), Gravity.CENTER))
        outer.addView(qrWrap, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = ViewUtils.dp(12)
            bottomMargin = ViewUtils.dp(20)
        })

        val urlRow = CardView(context).apply {
            cardElevation = 0f
            radius = ViewUtils.dp(14).toFloat()
            setCardBackgroundColor(ViewUtils.resolveColor(context, R.attr.colorSurfaceContainerLow))
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                outlineSpotShadowColor = 0
                outlineAmbientShadowColor = 0
            }
        }
        val urlInner = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ViewUtils.dp(14), ViewUtils.dp(12), ViewUtils.dp(10), ViewUtils.dp(12))
            background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
            setOnClickListener {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        }
        val urlTv = TextView(context).apply {
            text = link
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            setLineSpacing(ViewUtils.dp(2).toFloat(), 1f)
            setPadding(0, 0, ViewUtils.dp(10), 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        urlInner.addView(urlTv)
        val copyBtn = FrameLayout(context).apply {
            background = ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackgroundBorderless)
            setPadding(ViewUtils.dp(6), ViewUtils.dp(6), ViewUtils.dp(6), ViewUtils.dp(6))
            setOnClickListener { v ->
                v?.parent?.requestDisallowInterceptTouchEvent(true)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("", link)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        val copyIv = ImageView(context).apply {
            setImageResource(R.drawable.ic_copy_outline_24)
            imageTintList = android.content.res.ColorStateList.valueOf(
                ViewUtils.resolveColor(context, android.R.attr.textColorSecondary)
            )
        }
        copyBtn.addView(copyIv, FrameLayout.LayoutParams(ViewUtils.dp(20), ViewUtils.dp(20), Gravity.CENTER))
        urlInner.addView(copyBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        urlRow.addView(urlInner)
        outer.addView(urlRow, LinearLayout.LayoutParams(
            ViewUtils.dp(290),
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = ViewUtils.dp(12)
            rightMargin = ViewUtils.dp(12)
            bottomMargin = ViewUtils.dp(12)
        })

        val hintTv = TextView(context).apply {
            text = "Tap link to open · tap icon to copy"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        outer.addView(hintTv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = ViewUtils.dp(8)
        })

        setView(outer)
    }
}
