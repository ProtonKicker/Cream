package ru.ytkab0bp.beamklipper.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.PermissionsChecker
import ru.ytkab0bp.beamklipper.R

@Composable
fun PermissionScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    var batteryChecked by remember { mutableStateOf(PermissionsChecker.hasBatteryPerm()) }
    var notificationsChecked by remember { mutableStateOf(PermissionsChecker.hasNotificationPerm()) }
    var hideChannelChecked by remember { mutableStateOf(PermissionsChecker.isNotificationsChannelHidden()) }
    var sdcardChecked by remember { mutableStateOf(PermissionsChecker.isNotBrokenBySDCard()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryChecked = PermissionsChecker.hasBatteryPerm()
                notificationsChecked = PermissionsChecker.hasNotificationPerm()
                hideChannelChecked = PermissionsChecker.isNotificationsChannelHidden()
                sdcardChecked = PermissionsChecker.isNotBrokenBySDCard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsChecked = granted
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.AppName),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )

                PermissionRow(
                    title = stringResource(R.string.BatteryOptimizationExclusion),
                    checked = batteryChecked,
                    onRowClick = {
                        if (!batteryChecked) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            )
                        }
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionRow(
                        title = stringResource(R.string.Notifications),
                        checked = notificationsChecked,
                        onRowClick = {
                            if (!notificationsChecked) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
                if (PermissionsChecker.ENABLE_NOTIFICATIONS_CHANNEL_CHECK &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !PermissionsChecker.ignoreNotificationsChannel()
                ) {
                    PermissionRow(
                        title = stringResource(R.string.HideNotificationsChannel),
                        checked = hideChannelChecked,
                        onRowClick = {
                            if (!hideChannelChecked) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.HideNotificationsChannelInfo, context.getString(R.string.ServicesChannel)),
                                    Toast.LENGTH_SHORT
                                ).show()
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        .putExtra(Settings.EXTRA_CHANNEL_ID, KlipperApp.SERVICES_CHANNEL)
                                )
                            }
                        }
                    )
                }
                if (!PermissionsChecker.isNotBrokenBySDCard()) {
                    PermissionRow(
                        title = stringResource(R.string.NotOnSdcard),
                        checked = sdcardChecked,
                        onRowClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${KlipperApp.INSTANCE.packageName}"))
                            )
                            Toast.makeText(context, R.string.NotOnSdcardInfo, Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                ) {
                    Text(stringResource(R.string.Next), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    checked: Boolean,
    onRowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .padding(horizontal = 21.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = false
        )
    }
}
