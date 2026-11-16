package io.github.estebangocoalt.blocdenotas.ui.home

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
                val inboxDescription = stringResource(R.string.cd_inbox, inboxCount)
                IconButton(
                    onClick = onOpenInbox,
                    modifier = Modifier.semantics { contentDescription = inboxDescription },
                ) {
                    BadgedBox(
                        badge = {
                            if (inboxCount > 0) Badge { Text(inboxCount.toString()) }
                        },
                    ) {
                        Icon(Icons.Default.MailOutline, contentDescription = null)
                    }
                }
                IconButton(onClick = onOpenSettings) {
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
                    )
                }
            }

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
                        NoteCard(item = item, onClick = { onOpenNote(item.note.id) })
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNewNote,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.fab_new)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
}

@Composable
private fun NoteCard(item: NoteWithItems, onClick: () -> Unit) {
    val note = item.note
    val swatch = NoteColors.at(note.colorIndex)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
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
