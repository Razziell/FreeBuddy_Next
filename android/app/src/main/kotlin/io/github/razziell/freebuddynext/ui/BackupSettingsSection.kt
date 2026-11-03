package io.github.razziell.freebuddynext.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.razziell.freebuddynext.ui.appStringResource as stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.razziell.freebuddynext.R
import io.github.razziell.freebuddynext.data.SettingsBackup
import io.github.razziell.freebuddynext.data.SettingsBackupCodec
import io.github.razziell.freebuddynext.data.ThemeStore
import io.github.razziell.freebuddynext.widget.WidgetActionButton
import io.github.razziell.freebuddynext.widget.WidgetBackupStore
import io.github.razziell.freebuddynext.widget.WidgetStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BackupSettingsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pendingJson by rememberSaveable { mutableStateOf<String?>(null) }
    val pending = remember(pendingJson) { pendingJson?.let { SettingsBackupCodec.decode(it) } }
    fun message(id: Int) = Toast.makeText(context, id, Toast.LENGTH_LONG).show()

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && !busy) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    // Read at export time, not from an initial Compose placeholder layout.
                    val backup = SettingsBackup(
                        ThemeStore.interfaceLayout(context).first(),
                        WidgetBackupStore.capture(context),
                        WidgetBackupStore.favorites(context),
                        ThemeStore.lowBatteryAlertSettings(context).first(),
                    )
                    val json = SettingsBackupCodec.encode(backup)
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt"))
                        .bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                }
                message(R.string.backup_export_done)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                message(R.string.backup_export_failed)
            } finally { busy = false }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) scope.launch {
            busy = true
            try {
                pendingJson = withContext(Dispatchers.IO) {
                    val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            require(output.size() + read <= SettingsBackupCodec.MAX_FILE_BYTES)
                            output.write(buffer, 0, read)
                        }
                        output.toByteArray()
                    }
                    bytes.toString(Charsets.UTF_8).also { SettingsBackupCodec.decode(it) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                message(R.string.backup_import_failed)
            } finally { busy = false }
        }
    }

    Text(stringResource(R.string.backup_title), modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.backup_description), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetActionButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain", "application/octet-stream")) },
                    enabled = !busy && pending == null, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.backup_import))
                }
                WidgetActionButton(onClick = { export.launch(SettingsBackupCodec.FILE_NAME) },
                    enabled = !busy && pending == null, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.backup_export))
                }
            }
        }
    }
    pending?.let { backup ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingJson = null },
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = { Text(stringResource(if (backup.widgets == null) R.string.backup_restore_legacy else R.string.backup_restore_confirmation)) },
            confirmButton = {
                AppActionButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        try {
                            withContext(Dispatchers.IO) {
                                ThemeStore.setInterfaceLayout(context, backup.layout)
                                backup.widgets?.let { WidgetBackupStore.restore(context, it, requireNotNull(backup.favorites)) }
                                backup.lowBatteryAlert?.let { ThemeStore.setLowBatteryAlertSettings(context, it) }
                            }
                            pendingJson = null
                            // Persistence succeeded; report publication failure separately.
                            if (backup.widgets != null) {
                                try {
                                    WidgetStorage.updateAll(context)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    message(R.string.backup_refresh_failed)
                                    return@launch
                                }
                            }
                            message(R.string.backup_import_done)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            message(R.string.backup_restore_failed)
                        } finally { busy = false }
                    }
                }) { Text(stringResource(R.string.backup_restore)) }
            },
            dismissButton = {
                AppActionButton(enabled = !busy, onClick = { pendingJson = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
