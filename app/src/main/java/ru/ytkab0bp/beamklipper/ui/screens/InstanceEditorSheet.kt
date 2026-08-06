package ru.ytkab0bp.beamklipper.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.ui.state.InstanceEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstanceEditorSheet(
    editInstance: KlipperInstance?,
    onDismiss: () -> Unit,
    viewModel: InstanceEditorViewModel = viewModel()
) {
    val filesList by viewModel.filesList.collectAsState()
    val configFile by viewModel.configFile.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf(TextFieldValue(editInstance?.name ?: "")) }
    var autostart by remember { mutableStateOf(editInstance?.autostart ?: false) }
    var showConfigPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editInstance) {
        if (editInstance == null) viewModel.loadForCreate() else viewModel.loadForEdit(editInstance)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(if (editInstance == null) R.string.NewInstance else R.string.EditInstance),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.InstanceName),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Printer 1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            if (editInstance == null) {
                Text(
                    text = stringResource(R.string.InstanceConfig),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (filesList.isNotEmpty()) showConfigPicker = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = configFile ?: stringResource(R.string.InstanceConfigHint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (configFile != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openInstanceFolder(context, editInstance) }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.EditOpenDirectory),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.Autostart),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = autostart, onCheckedChange = { autostart = it })
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    var nameStr = name.text.trim()
                    if (nameStr.isEmpty()) {
                        val currentInstances = KlipperApp.DATABASE.getInstances().map { it.name }
                        var n = 1
                        while (true) {
                            val candidate = "Printer $n"
                            if (!currentInstances.contains(candidate)) {
                                nameStr = candidate
                                break
                            }
                            n++
                        }
                    }
                    if (editInstance == null && configFile.isNullOrEmpty()) {
                        error = context.getString(R.string.ErrorConfigEmpty)
                        return@Button
                    }
                    viewModel.save(nameStr, autostart) {
                        onDismiss()
                    }
                },
                enabled = !saving,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(if (editInstance == null) R.string.InstanceCreate else R.string.InstanceOK),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showConfigPicker) {
        AlertDialog(
            onDismissRequest = { showConfigPicker = false },
            title = { Text(stringResource(R.string.InstanceConfig)) },
            text = {
                Column {
                    filesList.forEach { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectConfig(f)
                                    showConfigPicker = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(f, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfigPicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(stringResource(R.string.Error)) },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = { error = null }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }
}

private fun openInstanceFolder(context: android.content.Context, instance: KlipperInstance) {
    val uri = android.provider.DocumentsContract.buildRootUri("ru.ytkab0bp.beamklipper", instance.id)
    try {
        try {
            try {
                context.startActivity(Intent("android.intent.action.VIEW").setDataAndType(uri, android.provider.DocumentsContract.Document.MIME_TYPE_DIR))
            } catch (_: android.content.ActivityNotFoundException) {
                context.startActivity(Intent("android.provider.action.BROWSE").setDataAndType(uri, android.provider.DocumentsContract.Document.MIME_TYPE_DIR))
            }
        } catch (_: android.content.ActivityNotFoundException) {
            context.startActivity(Intent("android.provider.action.BROWSE_DOCUMENT_ROOT").setDataAndType(uri, android.provider.DocumentsContract.Document.MIME_TYPE_DIR))
        }
    } catch (_: Throwable) {}
}
