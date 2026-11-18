package io.github.estebangocoalt.blocdenotas.ui.home

import io.github.estebangocoalt.blocdenotas.ui.common.PropertyChips
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import io.github.estebangocoalt.blocdenotas.data.SavedView
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.ui.editor.NoteTemplate
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.List
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import io.github.estebangocoalt.blocdenotas.ui.common.formatDueDate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import io.github.estebangocoalt.blocdenotas.ui.MainViewModel
import io.github.estebangocoalt.blocdenotas.ui.NoteFilter
import io.github.estebangocoalt.blocdenotas.ui.common.MiniChecklistRow
import io.github.estebangocoalt.blocdenotas.ui.common.labelRes
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors

@StringRes
private fun NoteFilter.labelRes(): Int = when (this) {
    NoteFilter.ALL -> R.string.filter_all
    NoteFilter.NOTES -> R.string.filter_notes
    NoteFilter.IDEAS -> R.string.filter_ideas
    NoteFilter.SHOPPING -> R.string.filter_shopping
    NoteFilter.TASKS -> R.string.filter_tasks
    NoteFilter.PINNED -> R.string.filter_pinned
}

@Composable
fun HomeScreen(
    onNewNote: () -> Unit,
    onNewFromTemplate: (NoteTemplate) -> Unit,
    onNewFromCustom: (io.github.estebangocoalt.blocdenotas.data.CustomTemplate) -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenInbox: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    vm: MainViewModel = viewModel(),
) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val inboxCount by vm.inboxCount.collectAsStateWithLifecycle()
    var templateMenu by remember { mutableStateOf(false) }
    var saveDialog by remember { mutableStateOf(false) }
    val savedViews by vm.savedViews.collectAsStateWithLifecycle()

    if (saveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text(stringResource(R.string.view_save_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.view_name_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        vm.saveCurrentView(name)
                        saveDialog = false
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { saveDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (templateMenu) {
        TemplateSheet(
            onPick = { template ->
                templateMenu = false
                onNewFromTemplate(template)
            },
            onPickCustom = { template ->
                templateMenu = false
                onNewFromCustom(template)
            },
            onBlank = {
                templateMenu = false
                onNewNote()
            },
            onDismiss = { templateMenu = false },
        )
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.title_notes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                val themePrefs = LocalContext.current.blocApp.themePrefs
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                IconButton(
                    onClick = { themePrefs.toggle(isDark) },
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_theme_toggle),
                        contentDescription = stringResource(R.string.cd_theme_toggle),
                    )
                }
                Spacer(Modifier.size(6.dp))
                Box {
                    IconButton(
                        onClick = { templateMenu = true },
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Icon(Icons.Default.List, contentDescription = stringResource(R.string.template_menu))
                    }
                }
                val inboxDescription = stringResource(R.string.cd_inbox, inboxCount)
                Spacer(Modifier.size(6.dp))
                IconButton(
                    onClick = onOpenInbox,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).semantics { contentDescription = inboxDescription },
                ) {
                    BadgedBox(
                        badge = {
                            if (inboxCount > 0) Badge { Text(inboxCount.toString()) }
                        },
                    ) {
                        Icon(Icons.Default.MailOutline, contentDescription = null)
                    }
                }
                Spacer(Modifier.size(6.dp))
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                }
            }

            TextField(
                value = query,
                onValueChange = vm::setQuery,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(NoteFilter.entries) { option ->
                    FilterChip(
                        selected = option == filter,
                        onClick = { vm.setFilter(option) },
                        label = { Text(stringResource(option.labelRes())) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = null,
                    )
                }
            }

            SavedViewsRow(
                views = savedViews,
                currentFilter = filter,
                currentQuery = query,
                onApply = vm::applyView,
                onSave = { saveDialog = true },
                onDelete = vm::deleteView,
            )

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.empty_notes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(notes, key = { it.note.id }) { item ->
                        NoteCard(item = item, onClick = { onOpenNote(item.note.id) }, onTagClick = vm::setQuery)
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNewNote,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.fab_new), fontWeight = FontWeight.SemiBold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
}

@Composable
private fun NoteCard(item: NoteWithItems, onClick: () -> Unit, onTagClick: (String) -> Unit) {
    val note = item.note
    val swatch = NoteColors.at(note.colorIndex)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = swatch.background,
            contentColor = swatch.content,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(note.kind.labelRes()).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = swatch.accent,
                    modifier = Modifier.weight(1f),
                )
                if (note.pinned) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = swatch.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (note.body.isNotBlank()) {
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            item.items.take(4).forEach { MiniChecklistRow(it, swatch) }
            if (item.items.isNotEmpty()) {
                val done = item.items.count { it.checked }
                LinearProgressIndicator(
                    progress = { done.toFloat() / item.items.size },
                    color = swatch.accent,
                    trackColor = swatch.accent.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                )
                Text(
                    text = "$done/${item.items.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = swatch.accent,
                )
            }
            PropertyChips(note.priority, note.tags, onTagClick = onTagClick)
            note.dueAt?.let { due ->
                Text(
                    text = formatDueDate(due),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = swatch.content,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            if (item.items.size > 4) {
                Text(
                    text = "+ ${item.items.size - 4}",
                    style = MaterialTheme.typography.labelMedium,
                    color = swatch.accent,
                )
            }
        }
    }
}

/** Vistas guardadas: chips con nombre que aplican un filtro y una búsqueda. Pulsación larga = eliminar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedViewsRow(
    views: List<SavedView>,
    currentFilter: NoteFilter,
    currentQuery: String,
    onApply: (SavedView) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
) {
    val canSave = currentFilter != NoteFilter.ALL || currentQuery.isNotBlank()
    if (views.isEmpty() && !canSave) return
    LazyRow(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(views, key = { it.id }) { view ->
            val selected = view.filter == currentFilter.name && view.query == currentQuery.trim()
            var menu by remember { mutableStateOf(false) }
            Box {
                Text(
                    text = view.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .combinedClickable(onClick = { onApply(view) }, onLongClick = { menu = true })
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.view_delete)) },
                        onClick = {
                            menu = false
                            onDelete(view.id)
                        },
                    )
                }
            }
        }
        if (canSave) {
            item(key = "save") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable(onClick = onSave)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.view_save),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}
