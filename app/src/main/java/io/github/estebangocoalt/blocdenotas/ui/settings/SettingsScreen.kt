package io.github.estebangocoalt.blocdenotas.ui.settings

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.ui.theme.ThemeMode
import android.net.Uri
import android.provider.Settings
import android.os.Build
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.backup.BackupManager
import io.github.estebangocoalt.blocdenotas.backup.ImportMode

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenTrash: () -> Unit,
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var pendingImport by rememberSaveable { mutableStateOf<Uri?>(null) }
    val snackbar = remember { SnackbarHostState() }

    // Selectores de archivo del sistema: no hace falta ningún permiso de almacenamiento.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> if (uri != null) vm.export(uri) }
    val markdownLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> if (uri != null) vm.exportMarkdown(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingImport = uri }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(context.getString(it.res, *it.args.toTypedArray()))
            vm.consumeMessage()
        }
    }

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            SectionLabel(R.string.settings_backup_section)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export)) },
                supportingContent = { Text(stringResource(R.string.settings_export_desc)) },
                leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                modifier = Modifier.clickable(enabled = !busy) {
                    exportLauncher.launch(BackupManager.suggestedFileName())
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export_md)) },
                supportingContent = { Text(stringResource(R.string.settings_export_md_desc)) },
                leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                modifier = Modifier.clickable(enabled = !busy) {
                    markdownLauncher.launch(BackupManager.suggestedMarkdownFileName())
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_import)) },
                supportingContent = { Text(stringResource(R.string.settings_import_desc)) },
                leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                modifier = Modifier.clickable(enabled = !busy) {
                    importLauncher.launch(IMPORT_MIME_TYPES)
                },
            )
            Text(
                text = stringResource(R.string.settings_backup_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.trash_title)) },
                supportingContent = { Text(stringResource(R.string.settings_trash_desc)) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenTrash),
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionLabel(R.string.settings_reminders_section)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_notifications)) },
                supportingContent = { Text(stringResource(R.string.settings_notifications_desc)) },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_exact_alarms)) },
                    supportingContent = { Text(stringResource(R.string.settings_exact_alarms_desc)) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.fromParts("package", context.packageName, null))
                        runCatching { context.startActivity(intent) }
                    },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionLabel(R.string.settings_appearance_section)
            val themePrefs = context.blocApp.themePrefs
            val themeMode by themePrefs.mode.collectAsStateWithLifecycle()
            val dynamic by themePrefs.dynamic.collectAsStateWithLifecycle()
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark,
                ).forEach { (option, label) ->
                    FilterChip(
                        selected = themeMode == option,
                        onClick = { themePrefs.setMode(option) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dynamic)) },
                    supportingContent = { Text(stringResource(R.string.settings_dynamic_desc)) },
                    trailingContent = {
                        Switch(checked = dynamic, onCheckedChange = { themePrefs.setDynamic(it) })
                    },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionLabel(R.string.settings_calendar_section)
            val calendarPrefs = context.blocApp.calendarPrefs
            val region by calendarPrefs.region.collectAsStateWithLifecycle()
            val showSaints by calendarPrefs.showSaints.collectAsStateWithLifecycle()
            var pickingRegion by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_holidays)) },
                supportingContent = { Text(stringResource(region.labelRes())) },
                modifier = Modifier.clickable { pickingRegion = true },
            )
            if (pickingRegion) {
                HolidayRegionDialog(
                    selected = region,
                    onSelect = { calendarPrefs.setRegion(it) },
                    onDismiss = { pickingRegion = false },
                )
            }
            Text(
                text = stringResource(R.string.settings_holidays_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_saints)) },
                supportingContent = { Text(stringResource(R.string.settings_saints_desc)) },
                trailingContent = {
                    Switch(checked = showSaints, onCheckedChange = { calendarPrefs.setShowSaints(it) })
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionLabel(R.string.settings_security_section)
            val lockEnabled by context.blocApp.appLock.enabled.collectAsStateWithLifecycle()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_lock)) },
                supportingContent = { Text(stringResource(R.string.settings_lock_desc)) },
                trailingContent = {
                    Switch(checked = lockEnabled, onCheckedChange = { vm.setLockEnabled(it) })
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionLabel(R.string.settings_language_section)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    supportingContent = { Text(stringResource(R.string.settings_language_desc)) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null))
                        runCatching { context.startActivity(intent) }
                    },
                )
            } else {
                Text(
                    text = stringResource(R.string.settings_language_system),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionLabel(R.string.settings_demo_section)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_demo_load)) },
                supportingContent = { Text(stringResource(R.string.settings_demo_load_desc)) },
                modifier = Modifier.clickable { vm.loadDemo() },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_demo_remove)) },
                modifier = Modifier.clickable { vm.removeDemo() },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionLabel(R.string.settings_about)
            Text(
                text = stringResource(R.string.settings_license),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.import_mode_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModeOption(R.string.import_mode_merge, R.string.import_mode_merge_desc) {
                        vm.import(uri, ImportMode.MERGE)
                        pendingImport = null
                    }
                    ModeOption(R.string.import_mode_replace, R.string.import_mode_replace_desc) {
                        vm.import(uri, ImportMode.REPLACE)
                        pendingImport = null
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private val IMPORT_MIME_TYPES = arrayOf(
    "application/zip",
    "application/x-zip-compressed",
    "application/json",
    "application/octet-stream",
)

@Composable
private fun SectionLabel(@StringRes res: Int) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ModeOption(@StringRes title: Int, @StringRes description: Int, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
