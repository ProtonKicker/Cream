package ru.ytkab0bp.beamklipper.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.service.WebService
import ru.ytkab0bp.beamklipper.ui.components.BrutalTile
import ru.ytkab0bp.beamklipper.ui.state.MainViewModel
import ru.ytkab0bp.beamklipper.ui.theme.Accent
import ru.ytkab0bp.beamklipper.ui.theme.Ink
import ru.ytkab0bp.beamklipper.ui.theme.InkMuted
import ru.ytkab0bp.beamklipper.ui.theme.Paper
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
                Surface(color = Paper) {
                    Box(Modifier.padding(top = 16.dp, bottom = 12.dp)) {
                        val isFluidd = webFrontend == Prefs.FRONTEND_FLUIDD
                        val running = webState == KlipperInstance.State.RUNNING
                        val webBg = if (running) Accent else Paper
                        BrutalTile(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp),
                            background = webBg,
                            onClick = if (running) ({ openWebFrontend(context) }) else null
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isFluidd) R.drawable.ic_square_stack_up_outline_28 else R.drawable.ic_sailing_24
                                    ),
                                    contentDescription = null,
                                    tint = Ink,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(if (isFluidd) R.string.Fluidd else R.string.Mainsail),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Ink,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (running) {
                                        Text(
                                            text = webIpInfo(context),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "instances-label", contentType = "label") {
                Text(
                    text = "INSTANCES",
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            items(
                instances.chunked(2),
                key = { it.joinToString("|") { inst -> inst.id ?: inst.name } },
                contentType = { "inst-row" }
            ) { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chunk.forEach { inst ->
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
                            val instBg = cardColor((inst.id ?: inst.name).hashCode())
                            val toggleBg = when (stateVal) {
                                KlipperInstance.State.RUNNING, KlipperInstance.State.STOPPING -> Accent
                                else -> Paper
                            }
                            BrutalTile(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 130.dp)
                                    .combinedClickable(
                                        onClick = onClickInst,
                                        onLongClick = onLongClickInst
                                    ),
                                background = instBg
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            painter = painterResource(inst.icon.drawable),
                                            contentDescription = null,
                                            tint = Ink,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = inst.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (stateVal == KlipperInstance.State.STARTING || stateVal == KlipperInstance.State.STOPPING || stateVal == KlipperInstance.State.RUNNING) {
                                        val statusText = when (stateVal) {
                                            KlipperInstance.State.STARTING -> stringResource(R.string.InstanceStarting)
                                            KlipperInstance.State.STOPPING -> stringResource(R.string.InstanceStopping)
                                            else -> "Running"
                                        }
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkMuted
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Surface(
                                            onClick = onToggleInst,
                                            enabled = stateVal != KlipperInstance.State.STARTING && stateVal != KlipperInstance.State.STOPPING,
                                            shape = RectangleShape,
                                            color = toggleBg,
                                            contentColor = Ink,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .border(2.dp, Ink, RectangleShape),
                                            tonalElevation = 0.dp,
                                            shadowElevation = 0.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    painterResource(if (stateVal == KlipperInstance.State.RUNNING || stateVal == KlipperInstance.State.STOPPING) R.drawable.ic_stop_24 else R.drawable.ic_play_28),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (chunk.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            val fabShape = RectangleShape
            Box(
                modifier = Modifier.size(72.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(fabShape)
                        .background(Accent, fabShape)
                        .border(2.dp, Ink, fabShape)
                        .let {
                            it.clickable(
                                onClick = {
                                    editorInstance = null
                                    editorVisible = true
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_add_outline_28),
                        contentDescription = stringResource(R.string.NewInstance),
                        tint = Ink,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            if (instances.isNotEmpty()) {
                Box(
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(fabShape)
                            .background(Accent, fabShape)
                            .border(2.dp, Ink, fabShape)
                            .let {
                                it.clickable(onClick = { mainViewModel.runStopAll() })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(if (anyRunning) R.drawable.ic_stop_24 else R.drawable.ic_play_28),
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }

    deleteInstance?.let { inst ->
        Dialog(onDismissRequest = { deleteInstance = null }) {
            val dlgShape = RectangleShape
            Box(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Paper, dlgShape)
                        .border(2.dp, Ink, dlgShape)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = stringResource(R.string.InstanceDelete, inst.name),
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.InstanceDeleteConfirm),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { deleteInstance = null }) {
                                Text(
                                    stringResource(android.R.string.cancel),
                                    color = Ink
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    mainViewModel.delete(inst)
                                    deleteInstance = null
                                }
                            ) {
                                Text(
                                    stringResource(android.R.string.ok),
                                    color = Ink
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (noFreeSlots) {
        Dialog(onDismissRequest = { noFreeSlots = false }) {
            val dlgShape = RectangleShape
            Box(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Paper, dlgShape)
                        .border(2.dp, Ink, dlgShape)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = stringResource(R.string.NoFreeSlots),
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.NoFreeSlotsDescription, KlipperInstance.SLOTS_COUNT),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { noFreeSlots = false }) {
                                Text(
                                    stringResource(android.R.string.ok),
                                    color = Ink
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editorVisible) {
        InstanceEditorSheet(
            editInstance = editorInstance,
            onDismiss = { editorVisible = false }
        )
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
