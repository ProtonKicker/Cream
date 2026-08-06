package ru.ytkab0bp.beamklipper.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.service.WebService
import ru.ytkab0bp.beamklipper.ui.state.MainViewModel
import ru.ytkab0bp.beamklipper.ui.theme.InstanceCardDark
import ru.ytkab0bp.beamklipper.ui.theme.InstanceCardIconTintDark
import ru.ytkab0bp.beamklipper.ui.theme.InstanceCardIconTintLight
import ru.ytkab0bp.beamklipper.ui.theme.InstanceCardLight
import ru.ytkab0bp.beamklipper.ui.theme.OnInstanceCardDark
import ru.ytkab0bp.beamklipper.ui.theme.OnInstanceCardLight
import ru.ytkab0bp.beamklipper.ui.theme.OnWebCardDark
import ru.ytkab0bp.beamklipper.ui.theme.OnWebCardLight
import ru.ytkab0bp.beamklipper.ui.theme.WebCardDarkFluidd
import ru.ytkab0bp.beamklipper.ui.theme.WebCardDarkMainsail
import ru.ytkab0bp.beamklipper.ui.theme.WebCardLightFluidd
import ru.ytkab0bp.beamklipper.ui.theme.WebCardLightMainsail
import ru.ytkab0bp.beamklipper.ui.theme.cardColor
import ru.ytkab0bp.beamklipper.utils.Prefs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    isCurrentLauncher: Boolean,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val instances by mainViewModel.instances.collectAsStateWithLifecycle()
    val instanceStates by mainViewModel.instanceStates.collectAsStateWithLifecycle()
    val webState by mainViewModel.webState.collectAsStateWithLifecycle()
    val webFrontend by mainViewModel.webFrontend.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    var deleteInstance by remember { mutableStateOf<KlipperInstance?>(null) }
    var noFreeSlots by remember { mutableStateOf(false) }
    var editorInstance by remember { mutableStateOf<KlipperInstance?>(null) }
    var editorVisible by remember { mutableStateOf(false) }
    val anyRunning by remember(instances, instanceStates) {
        derivedStateOf {
            instances.any {
                val s = instanceStates[it.id ?: it.name] ?: it.getState()
                s == KlipperInstance.State.RUNNING || s == KlipperInstance.State.STARTING
            }
        }
    }

    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, top = 0.dp, end = 20.dp, bottom = 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            stickyHeader(key = "web", contentType = "web") {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.padding(top = 16.dp, bottom = 12.dp)) {
                        WebCard(
                            frontend = webFrontend,
                            webState = webState,
                            darkTheme = darkTheme,
                            onClick = { openWebFrontend(context) }
                        )
                    }
                }
            }
            items(instances, key = { it.id ?: it.name }, contentType = { "inst" }) { inst ->
                key(inst.id ?: inst.name) {
                    val k = inst.id ?: inst.name
                    val stateSnapshot by rememberUpdatedState(instanceStates)
                    val stateVal by remember(instanceStates, k) {
                        derivedStateOf { stateSnapshot[k] ?: inst.getState() }
                    }
                    val onClickInst by rememberUpdatedState(onClick@{
                        editorInstance = inst
                        editorVisible = true
                    })
                    val onLongClickInst by rememberUpdatedState(onLongClick@{
                        deleteInstance = inst
                    })
                    val onToggleInst by rememberUpdatedState(onToggle@{
                        when (stateVal) {
                            KlipperInstance.State.STARTING, KlipperInstance.State.STOPPING -> {}
                            KlipperInstance.State.IDLE -> {
                                if (!KlipperInstance.hasFreeSlots()) {
                                    noFreeSlots = true
                                } else {
                                    mainViewModel.toggle(inst)
                                }
                            }
                            else -> mainViewModel.toggle(inst)
                        }
                    })
                    InstanceCard(
                        instance = inst,
                        state = stateVal,
                        darkTheme = darkTheme,
                        onClick = onClickInst,
                        onToggle = onToggleInst,
                        onLongClick = onLongClickInst
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    editorInstance = null
                    editorVisible = true
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(92.dp)
            ) {
                Icon(painterResource(R.drawable.ic_add_outline_28), contentDescription = stringResource(R.string.NewInstance), modifier = Modifier.size(40.dp))
            }
            if (instances.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { mainViewModel.runStopAll() },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(92.dp)
                ) {
                    Icon(
                        painterResource(if (anyRunning) R.drawable.ic_stop_24 else R.drawable.ic_play_28),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }

    deleteInstance?.let { inst ->
        AlertDialog(
            onDismissRequest = { deleteInstance = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text(stringResource(R.string.InstanceDelete, inst.name)) },
            text = { Text(stringResource(R.string.InstanceDeleteConfirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainViewModel.delete(inst)
                        deleteInstance = null
                    }
                ) {
                    Text(
                        stringResource(android.R.string.ok),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteInstance = null }) {
                    Text(
                        stringResource(android.R.string.cancel),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        )
    }

    if (noFreeSlots) {
        AlertDialog(
            onDismissRequest = { noFreeSlots = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text(stringResource(R.string.NoFreeSlots)) },
            text = { Text(stringResource(R.string.NoFreeSlotsDescription, KlipperInstance.SLOTS_COUNT)) },
            confirmButton = {
                TextButton(onClick = { noFreeSlots = false }) {
                    Text(
                        stringResource(android.R.string.ok),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        )
    }

    if (editorVisible) {
        InstanceEditorSheet(
            editInstance = editorInstance,
            onDismiss = { editorVisible = false }
        )
    }
}

@Composable
private fun WebCard(frontend: String, webState: KlipperInstance.State, darkTheme: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val isFluidd = frontend == Prefs.FRONTEND_FLUIDD
    val running = webState == KlipperInstance.State.RUNNING
    val cardColor = if (darkTheme) {
        if (isFluidd) WebCardDarkFluidd else WebCardDarkMainsail
    } else {
        if (isFluidd) WebCardLightFluidd else WebCardLightMainsail
    }
    val onCardColor = if (darkTheme) OnWebCardDark else OnWebCardLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = running, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    if (isFluidd) R.drawable.ic_square_stack_up_outline_28 else R.drawable.ic_sailing_24
                ),
                contentDescription = null,
                tint = onCardColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (isFluidd) R.string.Fluidd else R.string.Mainsail),
                    style = MaterialTheme.typography.bodyLarge,
                    color = onCardColor
                )
                if (running) {
                    Text(
                        text = webIpInfo(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = onCardColor.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstanceCard(
    instance: KlipperInstance,
    state: KlipperInstance.State,
    darkTheme: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val containerColor = if (darkTheme) InstanceCardDark else InstanceCardLight
    val onContainerColor = if (darkTheme) OnInstanceCardDark else OnInstanceCardLight
    val iconTint = if (darkTheme) InstanceCardIconTintDark else InstanceCardIconTintLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(instance.icon.drawable),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = instance.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = onContainerColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state == KlipperInstance.State.STARTING || state == KlipperInstance.State.STOPPING) {
                    Text(
                        text = stringResource(
                            if (state == KlipperInstance.State.STARTING) R.string.InstanceStarting else R.string.InstanceStopping
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainerColor.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                onClick = onToggle,
                enabled = state != KlipperInstance.State.STARTING && state != KlipperInstance.State.STOPPING,
                shape = RoundedCornerShape(50),
                color = onContainerColor,
                contentColor = containerColor,
                modifier = Modifier.size(40.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(if (state == KlipperInstance.State.RUNNING || state == KlipperInstance.State.STOPPING) R.drawable.ic_stop_24 else R.drawable.ic_play_28),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun openWebFrontend(context: Context) {
    val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val i = wm.connectionInfo.ipAddress
    val ip = if (i == 0 || !KlipperInstance.isWebServerRunning()) "127.0.0.1" else Formatter.formatIpAddress(i)
    val t = System.currentTimeMillis()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$ip:${WebService.PORT}/?t=$t"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    context.startActivity(intent)
}

private fun webIpInfo(context: Context): String {
    val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    return context.getString(R.string.IPInfo, Formatter.formatIpAddress(wm.connectionInfo.ipAddress), WebService.PORT)
}
