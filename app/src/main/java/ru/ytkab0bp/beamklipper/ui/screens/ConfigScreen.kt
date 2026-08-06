package ru.ytkab0bp.beamklipper.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.MainActivity
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.serial.KlipperProbeTable
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager
import ru.ytkab0bp.beamklipper.ui.components.BrutalButton
import ru.ytkab0bp.beamklipper.ui.components.BrutalSwitch
import ru.ytkab0bp.beamklipper.ui.components.BrutalTile
import ru.ytkab0bp.beamklipper.ui.state.SettingsViewModel
import ru.ytkab0bp.beamklipper.ui.theme.Accent
import ru.ytkab0bp.beamklipper.ui.theme.Ink
import ru.ytkab0bp.beamklipper.ui.theme.InkMuted
import ru.ytkab0bp.beamklipper.ui.theme.Paper
import ru.ytkab0bp.beamklipper.ui.theme.PaperAlt
import ru.ytkab0bp.beamklipper.utils.Prefs
import java.io.File

@Composable
fun ConfigScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val engine by viewModel.engine.collectAsStateWithLifecycle()
    val webFrontend by viewModel.webFrontend.collectAsStateWithLifecycle()
    val usbNaming by viewModel.usbNaming.collectAsStateWithLifecycle()
    val cameraEnabled by viewModel.cameraEnabled.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showListUsb by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshCameraSwitch(granted)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 40.dp)
    ) {
        BrutalSectionHeader(stringResource(R.string.EngineFrontend))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrutalTile(
                modifier = Modifier.weight(1f).height(140.dp),
                background = Accent,
                onClick = { viewModel.cycleEngine() }
            ) {
                Column {
                    Icon(
                        painter = painterResource(R.drawable.ic_memory_chip_outline_28),
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.FirmwareEngine),
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = viewModel.engineTitle(engine),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }
            }
            BrutalTile(
                modifier = Modifier.weight(1f).height(140.dp),
                background = Accent,
                onClick = { viewModel.cycleFrontend() }
            ) {
                Column {
                    Icon(
                        painter = painterResource(R.drawable.ic_sync_outline_28),
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.WebFrontend),
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = viewModel.frontendTitle(webFrontend),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        BrutalSectionHeader(stringResource(R.string.USB))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrutalTile(
                modifier = Modifier.weight(1f).height(140.dp),
                background = PaperAlt,
                onClick = { viewModel.cycleUsbNaming() }
            ) {
                Column {
                    Icon(
                        painter = painterResource(R.drawable.ic_usb_cable_28),
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.USBDeviceNaming),
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = viewModel.usbNamingTitle(usbNaming),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }
            }
            BrutalTile(
                modifier = Modifier.weight(1f).height(140.dp),
                background = PaperAlt,
                onClick = { showListUsb = true }
            ) {
                Column {
                    Icon(
                        painter = painterResource(R.drawable.ic_grid_layout_outline_28),
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.ListUSB),
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        BrutalSectionHeader(stringResource(R.string.Camera))
        BrutalSwitchRow(
            title = stringResource(R.string.EnableCamera),
            checked = cameraEnabled,
            onCheckedChange = { checked ->
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    viewModel.setCameraEnabled(checked)
                }
            }
        )

        Spacer(Modifier.height(28.dp))
        BrutalSectionHeader(stringResource(R.string.Other))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (context is MainActivity && context.isCurrentLauncher()) {
                BrutalTile(
                    modifier = Modifier.weight(1f).height(140.dp),
                    background = PaperAlt,
                    onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                ) {
                    Column {
                        Icon(
                            painter = painterResource(R.drawable.ic_services_outline_28),
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.SystemSettings),
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink
                        )
                    }
                }
            }
            BrutalTile(
                modifier = Modifier.weight(1f).height(140.dp),
                background = PaperAlt,
                onClick = { showQr = true }
            ) {
                Column {
                    Icon(
                        painter = painterResource(R.drawable.ic_download_outline_28),
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.OtherGetFirmware),
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        BrutalSectionHeader(stringResource(R.string.AppSettings))
        BrutalTile(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            background = Accent,
            onClick = { showLanguage = true }
        ) {
            Column {
                Icon(
                    painter = painterResource(R.drawable.ic_globe_outline_28),
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.AppLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = viewModel.languageTitle(appLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        BrutalSectionHeader(stringResource(R.string.About))
        BrutalTile(
            modifier = Modifier.fillMaxWidth(),
            background = PaperAlt
        ) {
            Column {
                Text(
                    text = stringResource(R.string.IntroTitle),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Ink
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.IntroText),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        BrutalTile(
            modifier = Modifier.fillMaxWidth(),
            background = Paper
        ) {
            Text(
                text = stringResource(R.string.PrivacyNote),
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted
            )
        }

        Spacer(Modifier.height(12.dp))
        BrutalTile(
            modifier = Modifier.fillMaxWidth(),
            background = PaperAlt
        ) {
            Column {
                Text(
                    text = stringResource(R.string.CreditsTitle),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Ink
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.CreditsText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        BrutalSectionHeader("Information")
        BrutalTile(
            modifier = Modifier.fillMaxWidth(),
            background = Paper,
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/utkabobr/BeamKlipper")))
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_external_link_outline_24),
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.OriginalBeamKlipper),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    painterResource(R.drawable.ic_chevron_right_28),
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        BrutalTile(
            modifier = Modifier.fillMaxWidth(),
            background = Paper,
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ProtonKicker/Cream")))
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_github_28),
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.GitHubRepo),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    painterResource(R.drawable.ic_chevron_right_28),
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showListUsb) {
        ListUsbDialog(context, onDismiss = { showListUsb = false })
    }
    if (showQr) {
        QRCodeDialog(
            link = "https://github.com/utkabobr/klipper/releases/tag/prebuilt-v0.12.0",
            onDismiss = { showQr = false }
        )
    }
    if (showLanguage) {
        LanguageDialog(onDismiss = { showLanguage = false })
    }
}

@Composable
private fun BrutalSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = InkMuted,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
}

@Composable
private fun BrutalSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    BrutalTile(
        modifier = Modifier.fillMaxWidth(),
        background = Paper,
        onClick = { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
                modifier = Modifier.weight(1f)
            )
            BrutalSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun BrutalAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Paper, RectangleShape)
                    .border(2.dp, Ink, RectangleShape)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    title()
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        text()
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        dismissButton?.let {
                            it()
                            Spacer(Modifier.width(8.dp))
                        }
                        confirmButton()
                    }
                }
            }
        }
    }
}

@Composable
private fun ListUsbDialog(context: Context, onDismiss: () -> Unit) {
    val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val list = mutableListOf<String>()
    for (dev in manager.deviceList.values) {
        val drv = KlipperProbeTable.getInstance().findDriver(dev)
        list.add(
            Integer.toHexString(dev.vendorId) + "/" + Integer.toHexString(dev.productId) +
                    " - " + dev.deviceName +
                    (if (drv != null) " - " + drv.name + "\n" +
                            File(KlipperApp.INSTANCE.filesDir, "serial/" + UsbSerialManager.getUID(dev)).absolutePath else "")
        )
    }
    BrutalAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ListUSBTitle), style = MaterialTheme.typography.titleLarge, color = Ink) },
        text = {
            if (list.isEmpty()) {
                Text(stringResource(R.string.ListUSBNoDevices), color = Ink)
            } else {
                Column {
                    list.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = Ink) }
                }
            }
        },
        confirmButton = {
            BrutalButton(
                text = stringResource(android.R.string.ok),
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun LanguageDialog(onDismiss: () -> Unit) {
    val options = listOf(
        KlipperApp.INSTANCE.getString(R.string.LanguageSystem),
        KlipperApp.INSTANCE.getString(R.string.LanguageEnglish),
        KlipperApp.INSTANCE.getString(R.string.LanguageRussian),
        KlipperApp.INSTANCE.getString(R.string.LanguageChineseSimplified),
        KlipperApp.INSTANCE.getString(R.string.LanguageChineseTraditional)
    )
    BrutalAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.AppLanguage), style = MaterialTheme.typography.titleLarge, color = Ink) },
        text = {
            Column {
                options.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RectangleShape)
                            .clickable {
                                Prefs.appLanguage = when (index) {
                                    0 -> Prefs.LANGUAGE_SYSTEM
                                    1 -> Prefs.LANGUAGE_ENGLISH
                                    2 -> Prefs.LANGUAGE_RUSSIAN
                                    3 -> Prefs.LANGUAGE_CHINESE_SIMPLIFIED
                                    else -> Prefs.LANGUAGE_CHINESE_TRADITIONAL
                                }
                                Prefs.applyAppLanguage()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink)
                    }
                }
            }
        },
        confirmButton = {
            BrutalButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss
            )
        }
    )
}
