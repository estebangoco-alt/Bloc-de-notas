package io.github.estebangocoalt.blocdenotas.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.BoardColumnEntity
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import io.github.estebangocoalt.blocdenotas.ui.common.formatDueDate
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors

private val ColumnWidth = 288.dp

/** Tablero estilo Trello: columnas con tarjetas (las tareas). */
@Composable
fun BoardScreen(
    onOpenCard: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: BoardViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf<BoardColumnEntity?>(null) }
    var deleting by remember { mutableStateOf<BoardColumnEntity?>(null) }
    var addingColumn by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            text = stringResource(R.string.board_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 12.dp),
        )
        LazyRow(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.columns, key = { it.id }) { column ->
                BoardColumn(
                    column = column,
                    allColumns = state.columns,
                    cards = state.cards[column.id].orEmpty(),
                    onOpenCard = onOpenCard,
                    onAddCard = { title -> vm.addCard(column.id, title) },
                    onMoveCard = { cardId, target -> vm.moveCard(cardId, target) },
                    onShiftCard = { cardId, delta -> vm.shiftCard(cardId, delta) },
                    onRename = { renaming = column },
                    onDelete = { deleting = column },
                    onShiftColumn = { delta -> vm.shiftColumn(column.id, delta) },
                )
            }
            item {
                OutlinedButton(
                    onClick = { addingColumn = true },
                    modifier = Modifier.width(ColumnWidth),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.board_add_column), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    if (addingColumn) {
        TextDialog(
            title = R.string.board_add_column,
            label = R.string.board_column_hint,
            initial = "",
            confirm = R.string.action_add,
            onConfirm = { vm.addColumn(it) },
            onDismiss = { addingColumn = false },
        )
    }
    renaming?.let { column ->
        TextDialog(
            title = R.string.board_rename_column,
            label = R.string.board_column_hint,
            initial = column.title,
            confirm = R.string.action_save,
            onConfirm = { vm.renameColumn(column, it) },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { column ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.board_delete_column_title, column.title)) },
            text = { Text(stringResource(R.string.board_delete_column_text)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteColumn(column.id)
                    deleting = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun BoardColumn(
    column: BoardColumnEntity,
    allColumns: List<BoardColumnEntity>,
    cards: List<NoteWithItems>,
    onOpenCard: (String) -> Unit,
    onAddCard: (String) -> Unit,
    onMoveCard: (cardId: String, targetColumnId: String) -> Unit,
    onShiftCard: (cardId: String, delta: Int) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShiftColumn: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(ColumnWidth).fillMaxHeight(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = column.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                Text(
                    text = cards.size.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_column_menu))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.board_rename_column)) },
                            onClick = { menu = false; onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.board_move_left)) },
                            onClick = { menu = false; onShiftColumn(-1) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.board_move_right)) },
                            onClick = { menu = false; onShiftColumn(1) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cards, key = { it.note.id }) { card ->
                    BoardCard(
                        card = card,
                        otherColumns = allColumns.filter { it.id != column.id },
                        onOpen = { onOpenCard(card.note.id) },
                        onMoveTo = { target -> onMoveCard(card.note.id, target) },
                        onShift = { delta -> onShiftCard(card.note.id, delta) },
                    )
                }
            }

            AddCardRow(onAdd = onAddCard)
        }
    }
}

@Composable
private fun BoardCard(
    card: NoteWithItems,
    otherColumns: List<BoardColumnEntity>,
    onOpen: () -> Unit,
    onMoveTo: (String) -> Unit,
    onShift: (Int) -> Unit,
) {
    val swatch = NoteColors.at(card.note.colorIndex)
    val title = card.note.title.ifBlank {
        card.note.body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
            .ifBlank { stringResource(R.string.board_untitled) }
    }
    val done = card.items.count { it.checked }

    Card(
        colors = CardDefaults.cardColors(containerColor = swatch.background, contentColor = swatch.content),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
                card.note.dueAt?.let { due ->
                    Text(
                        text = formatDueDate(due),
                        style = MaterialTheme.typography.labelMedium,
                        color = swatch.accent,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (card.items.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.board_checklist_progress, done, card.items.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = swatch.accent,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            var menu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_card_menu))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    otherColumns.forEach { target ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.board_move_to, target.title)) },
                            onClick = { menu = false; onMoveTo(target.id) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.board_move_up)) },
                        onClick = { menu = false; onShift(-1) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.board_move_down)) },
                        onClick = { menu = false; onShift(1) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCardRow(onAdd: (String) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    fun commit() {
        if (text.isNotBlank()) onAdd(text)
        text = ""
    }

    if (!adding) {
        TextButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.board_add_card), modifier = Modifier.padding(start = 8.dp))
        }
    } else {
        LaunchedEffect(Unit) { focus.requestFocus() }
        Column(Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.board_card_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { text = ""; adding = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
                TextButton(onClick = { commit() }, enabled = text.isNotBlank()) {
                    Text(stringResource(R.string.action_add))
                }
            }
        }
    }
}

@Composable
private fun TextDialog(
    title: Int,
    label: Int,
    initial: String,
    confirm: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = {
                    onConfirm(value)
                    onDismiss()
                },
            ) { Text(stringResource(confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
