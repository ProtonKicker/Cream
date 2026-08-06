package ru.ytkab0bp.beamklipper.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.MainActivity
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.serial.KlipperProbeTable
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager
import ru.ytkab0bp.beamklipper.ui.state.SettingsViewModel
import ru.ytkab0bp.beamklipper.utils.Prefs
import java.io.File

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val engine by viewModel.engine.collectAsStateWithLifecycle()
    val webFrontend by viewModel.webFrontend.collectAsStateWithLifecycle()
    val usbNaming by viewModel.usbNaming.collectAsStateWithLifecycle()
    val cameraEnabled by viewModel.cameraEnabled.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showListUsb by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }

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
        SectionHeader(stringResource(R.string.EngineFrontend))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SnippetCard(
                iconRes = R.drawable.ic_memory_chip_outline_28,
                title = stringResource(R.string.FirmwareEngine),
                value = viewModel.engineTitle(engine),
                onClick = { viewModel.cycleEngine() },
                modifier = Modifier.weight(1f)
            )
            SnippetCard(
                iconRes = R.drawable.ic_sync_outline_28,
                title = stringResource(R.string.WebFrontend),
                value = viewModel.frontendTitle(webFrontend),
                onClick = { viewModel.cycleFrontend() },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionHeader(stringResource(R.string.USB))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SnippetCard(
                iconRes = R.drawable.ic_usb_cable_28,
                title = stringResource(R.string.USBDeviceNaming),
                value = viewModel.usbNamingTitle(usbNaming),
                onClick = { viewModel.cycleUsbNaming() },
                modifier = Modifier.weight(1f)
            )
            SnippetCard(
                iconRes = R.drawable.ic_grid_layout_outline_28,
                title = stringResource(R.string.ListUSB),
                onClick = { showListUsb = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionHeader(stringResource(R.string.Camera))
        SwitchRow(
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
        SectionHeader(stringResource(R.string.Other))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (context is MainActivity && context.isCurrentLauncher()) {
                SnippetCard(
                    iconRes = R.drawable.ic_services_outline_28,
                    title = stringResource(R.string.SystemSettings),
                    onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) },
                    modifier = Modifier.weight(1f)
                )
            }
            SnippetCard(
                iconRes = R.drawable.ic_download_outline_28,
                title = stringResource(R.string.OtherGetFirmware),
                onClick = { showQr = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionHeader(stringResource(R.string.AppSettings))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SnippetCard(
                iconRes = R.drawable.ic_globe_outline_28,
                title = stringResource(R.string.AppLanguage),
                value = viewModel.languageTitle(appLanguage),
                onClick = { showLanguage = true },
                modifier = Modifier.weight(1f)
            )
            SnippetCard(
                iconRes = R.drawable.ic_moon_outline_28,
                title = stringResource(R.string.AppTheme),
                value = viewModel.themeTitle(appTheme),
                onClick = { showTheme = true },
                modifier = Modifier.weight(1f)
            )
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
    if (showTheme) {
        ThemeDialog(onDismiss = { showTheme = false })
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
}

@Composable
private fun SnippetCard(
    iconRes: Int,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .let {
                if (onClick != null) it.clickable(onClick = onClick) else it
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (value != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ListUSBTitle)) },
        text = {
            if (list.isEmpty()) {
                Text(stringResource(R.string.ListUSBNoDevices))
            } else {
                Column {
                    list.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.AppLanguage)) },
        text = {
            Column {
                options.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun ThemeDialog(onDismiss: () -> Unit) {
    val options = listOf(
        KlipperApp.INSTANCE.getString(R.string.ThemeSystem),
        KlipperApp.INSTANCE.getString(R.string.ThemeLight),
        KlipperApp.INSTANCE.getString(R.string.ThemeDark)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.AppTheme)) },
        text = {
            Column {
                options.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Prefs.appTheme = when (index) {
                                    0 -> Prefs.THEME_SYSTEM
                                    1 -> Prefs.THEME_LIGHT
                                    else -> Prefs.THEME_DARK
                                }
                                Prefs.applyAppTheme()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}
