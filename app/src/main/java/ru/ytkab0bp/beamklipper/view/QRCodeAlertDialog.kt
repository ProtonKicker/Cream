package ru.ytkab0bp.beamklipper.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.annotation.NonNull
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

        val fl = FrameLayout(context)
        val iv = ImageView(context)
        val encoder = QRGEncoder(link, null, QRGContents.Type.TEXT, ViewUtils.dp(250))
        encoder.setColorWhite(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary))
        encoder.setColorBlack(0)
        iv.setImageBitmap(encoder.bitmap)
        fl.addView(iv, FrameLayout.LayoutParams(ViewUtils.dp(250), ViewUtils.dp(250), Gravity.CENTER))
        setView(fl)
    }
}
