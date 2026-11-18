package io.github.estebangocoalt.blocdenotas.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.sync.SyncException
import io.github.estebangocoalt.blocdenotas.sync.SyncState
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Sincronización opcional con un servidor WebDAV / Nextcloud. Apagada hasta que se rellenan los datos. */
@Composable
internal fun SyncSection(modifier: Modifier = Modifier) {
    val app = LocalContext.current.blocApp
    val prefs = remember { app.syncPrefs }
    val state by app.syncManager.state.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf(prefs.url) }
    var user by remember { mutableStateOf(prefs.user) }
    var password by remember { mutableStateOf(prefs.password) }
    var auto by remember { mutableStateOf(prefs.autoOnOpen) }

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.sync_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it
                prefs.url = it
            },
            label = { Text(stringResource(R.string.sync_url)) },
            placeholder = { Text("https://cloud.ejemplo.org/remote.php/dav/files/usuario/Notas") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = user,
            onValueChange = {
                user = it
                prefs.user = it
            },
            label = { Text(stringResource(R.string.sync_user)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                prefs.password = it
            },
            label = { Text(stringResource(R.string.sync_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.sync_auto)) },
        supportingContent = { Text(stringResource(R.string.sync_auto_desc)) },
        trailingContent = {
            Switch(checked = auto, onCheckedChange = {
                auto = it
                prefs.autoOnOpen = it
            })
        },
    )
    val running = state is SyncState.Running
    Button(
        enabled = !running && url.isNotBlank() && user.isNotBlank() && password.isNotEmpty(),
        onClick = { app.repository.appScope.launch { app.syncManager.syncNow() } },
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(stringResource(if (running) R.string.sync_running else R.string.sync_now))
    }
    val status = when (val s = state) {
        SyncState.Idle -> prefs.lastSyncAt.takeIf { it > 0 }?.let {
            stringResource(R.string.sync_last, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)))
        }
        SyncState.Running -> null
        is SyncState.Done -> stringResource(R.string.sync_done, s.added, s.updated)
        is SyncState.Failed -> stringResource(s.reason.messageRes())
    }
    if (status != null) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state is SyncState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

private fun SyncException.Reason.messageRes(): Int = when (this) {
    SyncException.Reason.BAD_URL -> R.string.sync_err_url
    SyncException.Reason.UNAUTHORIZED -> R.string.sync_err_auth
    SyncException.Reason.NOT_FOUND -> R.string.sync_err_folder
    SyncException.Reason.NETWORK -> R.string.sync_err_network
    SyncException.Reason.CONFLICT -> R.string.sync_err_conflict
    SyncException.Reason.INVALID_REMOTE -> R.string.sync_err_invalid
    SyncException.Reason.NOT_CONFIGURED -> R.string.sync_err_config
    SyncException.Reason.SERVER -> R.string.sync_err_server
}
