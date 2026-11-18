package io.github.estebangocoalt.blocdenotas.ui.board

import io.github.estebangocoalt.blocdenotas.ui.common.SettingsButton
import io.github.estebangocoalt.blocdenotas.ui.common.PropertyChips
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
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
import io.github.estebangocoalt.blocdenotas.data.BoardEntity
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import io.github.estebangocoalt.blocdenotas.ui.common.formatDueDate
import io.github.estebangocoalt.blocdenotas.ui.common.TaskFilterBar
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors

private val ColumnWidth = 288.dp

/** Estado del arrastre de una tarjeta (pulsación larga y arrastrar). Coordenadas en la ventana. */
@Stable
private class BoardDrag {
    val columnBounds = mutableMapOf<String, Rect>()
    val cardBounds = mutableMapOf<String, Rect>()

    var cardId by mutableStateOf<String?>(null)
    var title by mutableStateOf("")
    var colorIndex by mutableStateOf(0)
    var grab by mutableStateOf(Offset.Zero)
    var pointer by mutableStateOf(Offset.Zero)
    var cardSize by mutableStateOf(IntSize.Zero)

    /** Columna más cercana en horizontal y posición dentro de ella según la altura del dedo. */
    fun target(cards: Map<String, List<NoteWithItems>>): Pair<String, Int>? {
        val x = pointer.x
        val column = columnBounds.entries.minByOrNull { (_, r) ->
            when {
                x < r.left -> r.left - x
                x > r.right -> x - r.right
                else -> 0f
            }
        } ?: return null
        val ids = cards[column.key].orEmpty().map { it.note.id }.filter { it != cardId }
        val index = ids.count { id -> (cardBounds[id]?.center?.y ?: Float.MAX_VALUE) < pointer.y }
        return column.key to index
    }
}

/** Tablero estilo Trello: columnas con tarjetas (las tareas). */
@Composable
fun BoardScreen(
    onOpenCard: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    vm: BoardViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf<BoardColumnEntity?>(null) }
    var deleting by remember { mutableStateOf<BoardColumnEntity?>(null) }
    var addingColumn by remember { mutableStateOf(false) }
    var boardMenu by remember { mutableStateOf(false) }
    var addingBoard by remember { mutableStateOf(false) }
    var renamingBoard by remember { mutableStateOf<BoardEntity?>(null) }
    var deletingBoard by remember { mutableStateOf<BoardEntity?>(null) }

    val drag = remember { BoardDrag() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var rootWidth by remember { mutableIntStateOf(0) }
    val finishDrag: () -> Unit = {
        val id = drag.cardId
        val target = drag.target(state.visible)
        if (id != null && target != null) vm.dropCard(id, target.first, target.second)
        drag.cardId = null
    }

    // Al acercar el dedo al borde, el tablero se desplaza para poder llegar a otras columnas.
    LaunchedEffect(drag.cardId) {
        if (drag.cardId == null) return@LaunchedEffect
        val edge = with(density) { 56.dp.toPx() }
        while (isActive) {
            val x = drag.pointer.x
            when {
                x > rootWidth - edge -> listState.scrollBy(16f)
                x < edge -> listState.scrollBy(-16f)
            }
            delay(16)
        }
    }

    Box(
        modifier.fillMaxSize().onGloballyPositioned {
            rootOrigin = it.positionInRoot()
            rootWidth = it.size.width
        },
    ) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.current?.title ?: stringResource(R.string.board_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.board_subtitle,
                        state.columns.size,
                        state.cards.values.sumOf { it.size },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsButton(onOpenSettings)
            if (state.current != null) {
                Box {
                    IconButton(onClick = { boardMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.board_options))
                    }
                    DropdownMenu(expanded = boardMenu, onDismissRequest = { boardMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.board_rename)) },
                            onClick = {
                                boardMenu = false
                                renamingBoard = state.current
                            },
                        )
                        if (state.boards.size > 1) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.board_delete)) },
                                onClick = {
                                    boardMenu = false
                                    deletingBoard = state.current
                                },
                            )
                        }
                    }
                }
            }
        }
        // Un chip por tablero (uno por proyecto) y «+ Nuevo tablero».
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.boards, key = { it.id }) { board ->
                FilterChip(
                    selected = board.id == state.current?.id,
                    onClick = { vm.selectBoard(board.id) },
                    label = { Text(board.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
            item {
                AssistChip(
                    onClick = { addingBoard = true },
                    label = { Text(stringResource(R.string.board_new)) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                )
            }
        }
        val query by vm.query.collectAsStateWithLifecycle()
        val views by vm.views.collectAsStateWithLifecycle()
        TaskFilterBar(
            query = query,
            tags = state.tags,
            onChange = vm::setQuery,
            views = views,
            onApplyView = vm::applyView,
            onSaveView = vm::saveView,
            onDeleteView = vm::deleteView,
            showSort = false,
        )
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.columns, key = { it.id }) { column ->
                BoardColumn(
                    column = column,
                    allColumns = state.columns,
                    cards = state.visible[column.id].orEmpty(),
                    onOpenCard = onOpenCard,
                    onAddCard = { title -> vm.addCard(column.id, title) },
                    onMoveCard = { cardId, target -> vm.moveCard(cardId, target) },
                    onShiftCard = { cardId, delta -> vm.shiftCard(cardId, delta) },
                    onRename = { renaming = column },
                    onDelete = { deleting = column },
                    onShiftColumn = { delta -> vm.shiftColumn(column.id, delta) },
                    drag = drag,
                    onDragEnd = finishDrag,
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
    if (drag.cardId != null) {
        val topLeft = drag.pointer - drag.grab - rootOrigin
        val swatch = NoteColors.at(drag.colorIndex)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
                .width(with(density) { drag.cardSize.width.toDp() })
                .graphicsLayer { rotationZ = 2f },
        ) {
            Text(
                text = drag.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = swatch.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
    }

    if (addingBoard) {
        TextDialog(
            title = R.string.board_new,
            label = R.string.board_name_hint,
            initial = "",
            confirm = R.string.action_add,
            onConfirm = { vm.addBoard(it) },
            onDismiss = { addingBoard = false },
        )
    }
    renamingBoard?.let { board ->
        TextDialog(
            title = R.string.board_rename,
            label = R.string.board_name_hint,
            initial = board.title,
            confirm = R.string.action_save,
            onConfirm = { vm.renameBoard(board, it) },
            onDismiss = { renamingBoard = null },
        )
    }
    deletingBoard?.let { board ->
        AlertDialog(
            onDismissRequest = { deletingBoard = null },
            title = { Text(stringResource(R.string.board_delete_title, board.title)) },
            text = { Text(stringResource(R.string.board_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteBoard(board)
                    deletingBoard = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingBoard = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
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
    drag: BoardDrag,
    onDragEnd: () -> Unit,
) {
    DisposableEffect(column.id) { onDispose { drag.columnBounds.remove(column.id) } }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .width(ColumnWidth)
            .fillMaxHeight()
            .onGloballyPositioned { drag.columnBounds[column.id] = it.boundsInRoot() },
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
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
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
                        drag = drag,
                        onDragEnd = onDragEnd,
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
    drag: BoardDrag,
    onDragEnd: () -> Unit,
) {
    val swatch = NoteColors.at(card.note.colorIndex)
    val cardId = card.note.id
    val endDrag by rememberUpdatedState(onDragEnd)
    DisposableEffect(cardId) { onDispose { drag.cardBounds.remove(cardId) } }
    val title = card.note.title.ifBlank {
        card.note.body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
            .ifBlank { stringResource(R.string.board_untitled) }
    }
    val done = card.items.count { it.checked }
    val currentTitle by rememberUpdatedState(title)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (drag.cardId == cardId) 0.3f else 1f }
            .onGloballyPositioned { drag.cardBounds[cardId] = it.boundsInRoot() }
            .pointerInput(cardId) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val bounds = drag.cardBounds[cardId] ?: return@detectDragGesturesAfterLongPress
                        drag.title = currentTitle
                        drag.colorIndex = card.note.colorIndex
                        drag.grab = offset
                        drag.pointer = bounds.topLeft + offset
                        drag.cardSize = IntSize(bounds.width.roundToInt(), bounds.height.roundToInt())
                        drag.cardId = cardId
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        drag.pointer += amount
                    },
                    onDragEnd = { endDrag() },
                    onDragCancel = { drag.cardId = null },
                )
            }
            .clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                card.note.dueAt?.let { due ->
                    Text(
                        text = formatDueDate(due),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = swatch.content,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(CircleShape)
                            .background(swatch.background)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                PropertyChips(card.note.priority, card.note.tags, modifier = Modifier.padding(top = 6.dp))
                if (card.items.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { done.toFloat() / card.items.size },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, end = 8.dp).fillMaxWidth().height(4.dp).clip(CircleShape),
                    )
                    Text(
                        text = stringResource(R.string.board_checklist_progress, done, card.items.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
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
