package ru.ytkab0bp.beamklipper.view

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.ytkab0bp.beamklipper.InstanceIcon
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.MainActivity
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.utils.ViewUtils
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceSwitchView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class InstanceEditorBottomSheet(
    private val editInstance: KlipperInstance? = null
) : BottomSheetDialogFragment() {

    private lateinit var iconRecycler: RecyclerView
    private lateinit var nameEdit: EditText
    private var configCard: MaterialCardView? = null
    private var configValue: TextView? = null
    private lateinit var editOpenDir: TextView
    private lateinit var autostartSwitch: PreferenceSwitchView
    private lateinit var continueBtn: TextView
    private var selectedIcon: InstanceIcon = editInstance?.icon ?: InstanceIcon.PRINTER
    private var configFile: String? = null
    private var filesList: List<String> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            val behavior = BottomSheetBehavior.from(sheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            sheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            sheet.requestLayout()
        }
        return dialog
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val ctx = requireContext()
        val nested = NestedScrollView(ctx).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewUtils.dp(24), ViewUtils.dp(8), ViewUtils.dp(24), ViewUtils.dp(24))
            clipToPadding = false
        }

        val handle = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewUtils.dp(40), ViewUtils.dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = ViewUtils.dp(6)
                bottomMargin = ViewUtils.dp(14)
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = ViewUtils.dp(2).toFloat()
                setColor(ViewUtils.resolveColor(ctx, R.attr.dividerColor))
            }
        }
        root.addView(handle)

        val titleTv = TextView(ctx).apply {
            setText(if (editInstance == null) R.string.NewInstance else R.string.EditInstance)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTextColor(ViewUtils.resolveColor(ctx, android.R.attr.textColorPrimary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        }
        root.addView(titleTv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(20)
            marginStart = ViewUtils.dp(4)
        })

        val nameLabel = TextView(ctx).apply {
            setText(R.string.InstanceName)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(ViewUtils.resolveColor(ctx, android.R.attr.textColorSecondary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            letterSpacing = 0.04f
        }
        root.addView(nameLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginStart = ViewUtils.dp(8)
            bottomMargin = ViewUtils.dp(8)
        })
        val nameCard = MaterialCardView(ctx).apply {
            radius = ViewUtils.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(ViewUtils.resolveColor(ctx, R.attr.colorSurfaceContainerLow))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(56)).apply {
                bottomMargin = ViewUtils.dp(20)
            }
        }
        nameEdit = EditText(ctx).apply {
            setText(editInstance?.name)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ViewUtils.resolveColor(ctx, android.R.attr.textColorPrimary))
            setHintTextColor(ViewUtils.resolveColor(ctx, android.R.attr.textColorSecondary))
            setPadding(ViewUtils.dp(16), 0, ViewUtils.dp(16), 0)
            background = null
            hint = "e.g. Printer 1"
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setSelection(text.length)
        }
        nameCard.addView(nameEdit)
        root.addView(nameCard)

        if (editInstance == null) {
            filesList = File(KlipperApp.INSTANCE.filesDir, "klipper/config").listFiles()?.map { it.name }?.sorted() ?: emptyList()
            configFile = filesList.firstOrNull { it.lowercase().contains("example") } ?: filesList.firstOrNull()
            val configLabel = TextView(ctx).apply {
                setText(R.string.InstanceConfig)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(ViewUtils.resolveColor(ctx, android.R.attr.textColorSecondary))
                typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
                letterSpacing = 0.04f
            }
            root.addView(configLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = ViewUtils.dp(8)
                bottomMargin = ViewUtils.dp(8)
            })
            val cCard = MaterialCardView(ctx).apply {
                radius = ViewUtils.dp(20).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(ViewUtils.resolveColor(ctx, R.attr.colorSurfaceContainerLow))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(56)).apply {
                    bottomMargin = ViewUtils.dp(8)
                }
            }
            configCard = cCard
            val configInner = FrameLayout(ctx).apply {
                setPadding(ViewUtils.dp(16), 0, ViewUtils.dp(12), 0)
                background = ViewUtils.resolveDrawable(ctx, android.R.attr.selectableItemBackground)
                setOnClickListener {
                    if (filesList.isNotEmpty()) {
                        MaterialAlertDialogBuilder(ctx)
                            .setTitle(R.string.InstanceConfig)
                            .setItems(filesList.toTypedArray()) { _, which ->
                                configFile = filesList[which]
                                configValue?.text = configFile
                                configValue?.setTextColor(ViewUtils.resolveColor(ctx, android.R.attr.textColorPrimary))
                            }
                            .show()
                    }
                }
            }
            val emptyHint = ctx.getString(R.string.InstanceConfigHint)
            val cValue = TextView(ctx).apply {
                text = configFile ?: emptyHint
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTextColor(
                    if (configFile.isNullOrEmpty())
                        ViewUtils.resolveColor(ctx, android.R.attr.textColorSecondary)
                    else
                        ViewUtils.resolveColor(ctx, android.R.attr.textColorPrimary)
                )
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(0, 0, ViewUtils.dp(32), 0)
            }
            configValue = cValue
            val chevron = ImageView(ctx).apply {
                setImageResource(R.drawable.ic_chevron_up_outline_28)
                imageTintList = android.content.res.ColorStateList.valueOf(
                    ViewUtils.resolveColor(ctx, android.R.attr.textColorSecondary)
                )
                rotation = 270f
            }
            configInner.addView(cValue, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            configInner.addView(chevron, FrameLayout.LayoutParams(ViewUtils.dp(24), ViewUtils.dp(24), Gravity.END or Gravity.CENTER_VERTICAL))
            cCard.addView(configInner)
            root.addView(cCard)
        }

        editOpenDir = TextView(ctx).apply {
            setText(R.string.EditOpenDirectory)
            setTextColor(ViewUtils.resolveColor(ctx, android.R.attr.textColorPrimary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(ViewUtils.dp(16), 0, ViewUtils.dp(16), 0)
            background = ViewUtils.resolveDrawable(ctx, android.R.attr.selectableItemBackground)
            visibility = if (editInstance != null) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(56)).apply {
                bottomMargin = if (editInstance != null) ViewUtils.dp(8) else 0
            }
            setOnClickListener {
                val inst = editInstance ?: return@setOnClickListener
                val uri = android.provider.DocumentsContract.buildRootUri("ru.ytkab0bp.beamklipper", inst.id)
                try {
                    try {
                        try {
                            ctx.startActivity(android.content.Intent("android.intent.action.VIEW").setDataAndType(uri, android.provider.DocumentsContract.Document.MIME_TYPE_DIR))
                        } catch (_: android.content.ActivityNotFoundException) {
                            ctx.startActivity(android.content.Intent("android.provider.action.BROWSE").setDataAndType(uri, android.provider.DocumentsContract.Document.MIME_TYPE_DIR))
                        }
                    } catch (_: android.content.ActivityNotFoundException) {
                        ctx.startActivity(android.content.Intent("android.provider.action.BROWSE_DOCUMENT_ROOT").setDataAndType(uri, android.provider.DocumentsContract.Document.MIME_TYPE_DIR))
                    }
                } catch (_: Throwable) {}
            }
        }
        root.addView(editOpenDir)

        val iconsLabel = TextView(ctx).apply {
            setText(R.string.SelectIcon)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(ViewUtils.resolveColor(ctx, android.R.attr.textColorSecondary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            letterSpacing = 0.06f
        }
        root.addView(iconsLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(12)
            marginStart = ViewUtils.dp(4)
        })

        iconRecycler = RecyclerView(ctx).apply {
            layoutManager = GridLayoutManager(ctx, 4)
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val iconsList = InstanceIcon.values()
        iconRecycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card = MaterialCardView(ctx).apply {
                    radius = ViewUtils.dp(18).toFloat()
                    cardElevation = 0f
                    setCardBackgroundColor(ViewUtils.resolveColor(ctx, R.attr.colorSurfaceContainerLow))
                    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(80)).apply {
                        setMargins(ViewUtils.dp(5), ViewUtils.dp(5), ViewUtils.dp(5), ViewUtils.dp(5))
                    }
                    isCheckable = true
                }
                val frame = FrameLayout(ctx).apply {
                    setPadding(ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8))
                    background = ViewUtils.resolveDrawable(ctx, android.R.attr.selectableItemBackground)
                }
                val inner = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                }
                val iv = ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28))
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        ViewUtils.resolveColor(ctx, android.R.attr.textColorPrimary)
                    )
                }
                inner.addView(iv)
                frame.addView(inner)
                card.addView(frame)
                return object : RecyclerView.ViewHolder(card) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val icon = iconsList[position]
                val card = holder.itemView as MaterialCardView
                (card.getChildAt(0) as FrameLayout).let { fl ->
                    ((fl.getChildAt(0) as LinearLayout).getChildAt(0) as ImageView).setImageResource(icon.drawable)
                }
                card.isChecked = selectedIcon == icon
                card.strokeWidth = if (selectedIcon == icon) ViewUtils.dp(2) else 0
                if (selectedIcon == icon) {
                    card.setStrokeColor(android.content.res.ColorStateList.valueOf(ViewUtils.resolveColor(ctx, R.attr.colorPrimary)))
                }
                card.setOnClickListener {
                    selectedIcon = icon
                    notifyItemRangeChanged(0, itemCount)
                }
            }

            override fun getItemCount(): Int = iconsList.size
        }
        root.addView(iconRecycler, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ViewUtils.dp(28)
        })

        autostartSwitch = PreferenceSwitchView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(56)).apply {
                topMargin = ViewUtils.dp(8)
                bottomMargin = ViewUtils.dp(16)
            }
            bind(getString(R.string.Autostart), null, editInstance?.autostart ?: false)
            setOnClickListener { isChecked = !isChecked }
        }
        root.addView(autostartSwitch)

        val btnCard = MaterialCardView(ctx).apply {
            radius = ViewUtils.dp(26).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(ViewUtils.resolveColor(ctx, R.attr.colorPrimary))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(56))
        }
        continueBtn = TextView(ctx).apply {
            setText(if (editInstance == null) R.string.InstanceCreate else R.string.InstanceOK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ViewUtils.resolveColor(ctx, R.attr.colorOnPrimary))
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            gravity = Gravity.CENTER
            background = ViewUtils.resolveDrawable(ctx, android.R.attr.selectableItemBackground)
            setOnClickListener { saveAndDismiss() }
        }
        btnCard.addView(continueBtn)
        root.addView(btnCard)

        nested.addView(root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return nested
    }

    private fun saveAndDismiss() {
        var nameStr = nameEdit.text?.toString()?.trim() ?: ""
        if (TextUtils.isEmpty(nameStr)) {
            val instances = KlipperInstance.getInstances()
            var counter = 1
            while (true) {
                val candidate = "Printer " + counter
                if (instances.none { it.name == candidate }) {
                    nameStr = candidate
                    break
                }
                counter++
            }
        }

        if (editInstance != null) {
            val editing = editInstance
            editing.name = nameStr
            editing.autostart = autostartSwitch.isChecked
            editing.icon = selectedIcon
            KlipperApp.appScope.launch(Dispatchers.IO) {
                KlipperApp.DATABASE.update(editing)
            }
            dismiss()
            return
        }

        if (TextUtils.isEmpty(configFile)) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.Error)
                .setMessage(R.string.ErrorConfigEmpty)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        val inst = KlipperInstance().apply {
            id = UUID.randomUUID().toString()
            name = nameStr
            autostart = autostartSwitch.isChecked
            icon = selectedIcon
        }
        val cfg = File(inst.publicDirectory, "config/printer.cfg")
        val cfgText = configFile!!
        continueBtn.isEnabled = false
        KlipperApp.appScope.launch(Dispatchers.IO) {
            try {
                cfg.parentFile?.mkdirs()
                try {
                    FileInputStream(File(KlipperApp.INSTANCE.filesDir, "klipper/config/$cfgText")).use { fis ->
                        FileOutputStream(cfg).use { fos -> fis.copyTo(fos) }
                    }
                } catch (e: Exception) {
                    Log.w("InstanceEditor", "Failed to copy config file", e)
                }
                KlipperApp.DATABASE.insert(inst)
            } finally {
                activity?.runOnUiThread {
                    continueBtn.isEnabled = true
                }
            }
        }
        dismiss()
    }

    companion object {
        fun show(activity: MainActivity, editInstance: KlipperInstance? = null) {
            InstanceEditorBottomSheet(editInstance).show(activity.supportFragmentManager, "instance_editor")
        }
    }
}
