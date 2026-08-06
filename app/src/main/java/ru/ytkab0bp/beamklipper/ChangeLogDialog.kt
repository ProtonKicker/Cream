package ru.ytkab0bp.beamklipper

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Locale

class ChangeLogDialog(context: Context) : Dialog(context) {
    private val changelogText: String? = runCatching {
        context.assets.open("update.json").use { inp ->
            val obj = JSONObject(inp.readBytes().toString(StandardCharsets.UTF_8))
            val code = Locale.getDefault().language
            if (obj.has(code)) obj.getString(code) else obj.getString("en")
        }
    }.getOrNull()

    override fun show() {
        if (changelogText == null) {
            dismiss()
            return
        }
        setContentView(
            androidx.compose.ui.platform.ComposeView(context).apply {
                setContent {
                    ru.ytkab0bp.beamklipper.ui.theme.CreamTheme {
                        ChangelogHost(changelogText!!) { dismiss() }
                    }
                }
            }
        )
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.BOTTOM)
        super.show()
    }
}

@Composable
private fun ChangelogHost(text: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.Changelog),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.ChangelogOK))
                }
            }
        }
    }
}
