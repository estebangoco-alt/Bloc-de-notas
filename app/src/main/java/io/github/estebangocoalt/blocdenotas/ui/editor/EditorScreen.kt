package io.github.estebangocoalt.blocdenotas.ui.editor

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.estebangocoalt.blocdenotas.data.LinkLogic
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import io.github.estebangocoalt.blocdenotas.data.Aisle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.ui.common.labelRes
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteSwatch

@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onShoppingMode: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onNewSubpage: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: EditorViewModel = viewModel(),
) {
    val swatch = NoteColors.at(vm.colorIndex)
    var confirmDelete by remember { mutableStateOf(false) }
    var showDone by remember { mutableStateOf(true) }
    val allNotes by vm.allNotes.collectAsStateWithLifecycle()
    var bodyField by remember { mutableStateOf(TextFieldValue(vm.body)) }
    var pickerMention by remember { mutableStateOf<Boolean?>(null) }
    var moreMenu by remember { mutableStateOf(false) }
    var templateDialog by remember { mutableStateOf(false) }

    // El cuerpo se carga después de abrir la pantalla: mantiene el campo de texto al día.
    LaunchedEffect(vm.body) {
        if (bodyField.text != vm.body) bodyField = TextFieldValue(vm.body, TextRange(vm.body.length))
    }
    fun setBody(text: String) {
        bodyField = TextFieldValue(text, TextRange(text.length))
        vm.body = text
    }
    fun openNote(id: String) {
        vm.saveOnExit()
        onOpenNote(id)
    }

    // Guardado automático: al salir de la pantalla y cuando la app pasa a segundo plano.
    DisposableEffect(Unit) { onDispose { vm.saveOnExit() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.saveOnExit() }

    // Los chips, botones y campos usan los colores de la nota, no los del tema: así se leen
    // igual en claro y en oscuro (antes salía texto claro sobre el fondo pastel).
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = swatch.background,
            background = swatch.background,
            onSurface = swatch.content,
            onBackground = swatch.content,
            onSurfaceVariant = swatch.content.copy(alpha = 0.75f),
            outline = swatch.content.copy(alpha = 0.45f),
            outlineVariant = swatch.content.copy(alpha = 0.25f),
            primary = swatch.accent,
            secondaryContainer = swatch.accent.copy(alpha = 0.18f),
            onSecondaryContainer = swatch.content,
            surfaceVariant = swatch.content.copy(alpha = 0.08f),
        ),
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = swatch.background,
            contentColor = swatch.content,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                    Spacer(Modifier.weight(1f))
                    if (vm.kind == NoteKind.SHOPPING) {
                        IconButton(onClick = {
                            vm.saveOnExit()
                            onShoppingMode(vm.currentId)
                        }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = stringResource(R.string.shopping_mode))
                        }
                    }
                    IconButton(onClick = { vm.pinned = !vm.pinned }) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = stringResource(if (vm.pinned) R.string.cd_unpin else R.string.cd_pin),
                            tint = if (vm.pinned) swatch.accent else swatch.content.copy(alpha = 0.4f),
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete))
                    }
                    Box {
                        IconButton(onClick = { moreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more))
                        }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.template_save_as)) },
                                onClick = {
                                    moreMenu = false
                                    templateDialog = true
                                },
                            )
                        }
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                ) {
                    allNotes.firstOrNull { it.id == vm.parentId }?.let { parent ->
                        ParentChip(parent, swatch, onOpen = ::openNote)
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NoteKind.entries.forEach { option ->
                            FilterChip(
                                selected = vm.kind == option,
                                onClick = { vm.changeKind(option) },
                                label = { Text(stringResource(option.labelRes())) },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = swatch.content,
                                    selectedLabelColor = swatch.content,
                                    selectedContainerColor = Color.White.copy(alpha = 0.6f),
                                ),
                            )
                        }
                    }

                    val knownTags by vm.knownTags.collectAsStateWithLifecycle()
                    TagEditor(
                        tags = io.github.estebangocoalt.blocdenotas.data.TagLogic.parse(vm.tags),
                        known = knownTags,
                        onAdd = { vm.tags = io.github.estebangocoalt.blocdenotas.data.TagLogic.add(vm.tags, it) },
                        onRemove = { vm.tags = io.github.estebangocoalt.blocdenotas.data.TagLogic.remove(vm.tags, it) },
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    if (vm.kind == NoteKind.TASK) {
                        TaskOptions(
                            dueAt = vm.dueAt,
                            completed = vm.completed,
                            swatch = swatch,
                            onCompletedChange = { vm.completed = it },
                            onDueChange = {
                                vm.dueAt = it
                                if (it == null) {
                                    vm.reminderMinute = null
                                    vm.repeatRule = io.github.estebangocoalt.blocdenotas.data.RepeatRule.NONE
                                }
                            },
                            reminderMinute = vm.reminderMinute,
                            onReminderChange = { vm.reminderMinute = it },
                            repeatRule = vm.repeatRule,
                            onRepeatChange = { vm.repeatRule = it },
                            priority = vm.priority,
                            onPriorityChange = { vm.priority = it },
                        )
                        DependencyEditor(
                            currentId = vm.currentId,
                            dependsOn = vm.dependsOn,
                            all = allNotes,
                            swatch = swatch,
                            onChange = { vm.dependsOn = it },
                        )
                    }

                    NoteField(
                        value = vm.title,
                        onValueChange = { vm.title = it },
                        hint = stringResource(R.string.editor_title_hint),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        swatch = swatch,
                    )
                    TextField(
                        value = bodyField,
                        onValueChange = {
                            bodyField = it
                            vm.body = it.text
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        placeholder = {
                            Text(stringResource(R.string.editor_body_hint), style = MaterialTheme.typography.bodyLarge)
                        },
                        colors = noteFieldColors(swatch),
                    )

                    // Comandos «/»: aparecen al escribir «/» al final del texto.
                    val slash = LinkLogic.slashQuery(vm.body)
                    val commands = SlashCommand.entries
                        .map { it to stringResource(it.label) }
                        .filter { (_, label) -> slash != null && label.contains(slash, ignoreCase = true) }
                    if (slash != null && commands.isNotEmpty()) {
                        Column(
                            Modifier
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.6f)),
                        ) {
                            commands.take(6).forEach { (command, label) ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val base = LinkLogic.removeSlash(vm.body)
                                            val lineStart = base.isEmpty() || base.endsWith("\n")
                                            when (command) {
                                                SlashCommand.TODO -> {
                                                    setBody(base)
                                                    vm.addItem()
                                                }
                                                SlashCommand.HEADING -> setBody("$base# ")
                                                SlashCommand.BULLETS -> setBody("$base- ")
                                                SlashCommand.QUOTE -> setBody("$base> ")
                                                SlashCommand.DIVIDER ->
                                                    setBody(base + (if (lineStart) "" else "\n") + "---\n")
                                                SlashCommand.DATE -> setBody(
                                                    base + LocalDate.now()
                                                        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                                                )
                                                SlashCommand.TIME -> setBody(
                                                    base + LocalTime.now()
                                                        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                                                )
                                                SlashCommand.LINK -> {
                                                    setBody(base)
                                                    pickerMention = false
                                                }
                                                SlashCommand.MENTION -> {
                                                    setBody(base)
                                                    pickerMention = true
                                                }
                                                SlashCommand.SUBPAGE -> {
                                                    setBody(base)
                                                    vm.prepareForSubpage()
                                                    onNewSubpage(vm.currentId)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            }
                        }
                    }

                    vm.items.filter { !it.checked }.forEach { item ->
                        key(item.id) {
                            ItemRow(item = item, swatch = swatch, showAisle = vm.kind == NoteKind.SHOPPING, onRemove = { vm.removeItem(item) })
                        }
                    }

                    TextButton(
                        onClick = vm::addItem,
                        colors = ButtonDefaults.textButtonColors(contentColor = swatch.accent),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.editor_add_item))
                    }

                    val doneItems = vm.items.filter { it.checked }
                    if (doneItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDone = !showDone }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (showDone) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = swatch.accent,
                            )
                            Text(
                                text = stringResource(R.string.editor_completed, doneItems.size),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = swatch.accent,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        if (showDone) {
                            doneItems.forEach { item ->
                                key(item.id) {
                                    ItemRow(item = item, swatch = swatch, showAisle = vm.kind == NoteKind.SHOPPING, onRemove = { vm.removeItem(item) })
                                }
                            }
                        }
                    }

                    AttachmentsSection(vm = vm, swatch = swatch)

                    NoteRelations(
                        body = vm.body,
                        currentId = vm.currentId,
                        currentTitle = vm.title,
                        all = allNotes,
                        swatch = swatch,
                        onOpen = ::openNote,
                        onCreateLinked = { title -> vm.openLinked(title, onOpenNote) },
                        onNewSubpage = {
                            vm.prepareForSubpage()
                            onNewSubpage(vm.currentId)
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.35f)).padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    NoteColors.all.forEachIndexed { index, option ->
                        val selected = vm.colorIndex == index
                        val description = stringResource(R.string.cd_color, index + 1)
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(option.background)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) option.accent else swatch.content.copy(alpha = 0.3f),
                                    shape = CircleShape,
                                )
                                .clickable(role = Role.RadioButton) { vm.colorIndex = index }
                                .semantics { contentDescription = description },
                        )
                    }
                }
            }
        }
    }

    pickerMention?.let { mention ->
        var filter by remember { mutableStateOf("") }
        val candidates = allNotes.filter {
            it.id != vm.currentId && it.title.isNotBlank() && it.title.contains(filter, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { pickerMention = null },
            title = { Text(stringResource(if (mention) R.string.slash_mention else R.string.note_picker_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.search_hint)) },
                    )
                    if (candidates.isEmpty()) {
                        Text(
                            text = stringResource(R.string.note_picker_empty),
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    LazyColumn(Modifier.heightIn(max = 280.dp).padding(top = 8.dp)) {
                        items(candidates.take(40), key = { it.id }) { note ->
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val prefix = if (mention) "@" else ""
                                        setBody(vm.body + prefix + LinkLogic.link(note.title) + " ")
                                        pickerMention = null
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickerMention = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (templateDialog) {
        var templateName by remember { mutableStateOf(vm.title.trim()) }
        AlertDialog(
            onDismissRequest = { templateDialog = false },
            title = { Text(stringResource(R.string.template_save_as)) },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it.take(40) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.template_name)) },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = templateName.isNotBlank(),
                    onClick = {
                        vm.saveAsTemplate(templateName)
                        templateDialog = false
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { templateDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        vm.discard()
                        onBack()
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ItemRow(item: EditItem, swatch: NoteSwatch, showAisle: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (item.checked) {
                        Modifier.background(swatch.accent)
                    } else {
                        Modifier.border(2.dp, swatch.accent, RoundedCornerShape(8.dp))
                    },
                )
                .clickable { item.checked = !item.checked },
            contentAlignment = Alignment.Center,
        ) {
            if (item.checked) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        TextField(
            value = item.text,
            onValueChange = { item.text = it },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            ),
            placeholder = { Text(stringResource(R.string.editor_item_hint)) },
            colors = noteFieldColors(swatch),
        )
        if (showAisle && item.text.isNotBlank()) AislePicker(item, swatch)
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_remove_item),
                tint = swatch.content.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    textStyle: TextStyle,
    swatch: NoteSwatch,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = textStyle,
        placeholder = { Text(hint, style = textStyle) },
        colors = noteFieldColors(swatch),
    )
}

@Composable
private fun noteFieldColors(swatch: NoteSwatch): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedTextColor = swatch.content,
    unfocusedTextColor = swatch.content,
    cursorColor = swatch.accent,
    focusedPlaceholderColor = swatch.content.copy(alpha = 0.55f),
    unfocusedPlaceholderColor = swatch.content.copy(alpha = 0.55f),
)

/** Pasillo del artículo: muestra el actual (elegido o deducido) y permite cambiarlo. */
@Composable
private fun AislePicker(item: EditItem, swatch: NoteSwatch) {
    var open by remember { mutableStateOf(false) }
    val current = Aisle.resolve(item.text, item.aisle)
    Box {
        Text(
            text = stringResource(current.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = swatch.content,
            maxLines = 1,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (item.aisle.isEmpty()) 0.35f else 0.7f))
                .clickable { open = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.aisle_auto)) },
                onClick = {
                    item.aisle = ""
                    open = false
                },
            )
            Aisle.entries.forEach { aisle ->
                DropdownMenuItem(
                    text = { Text(stringResource(aisle.labelRes())) },
                    onClick = {
                        item.aisle = aisle.name
                        open = false
                    },
                )
            }
        }
    }
}
