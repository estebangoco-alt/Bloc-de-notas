package io.github.estebangocoalt.blocdenotas.ui.editor

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
    modifier: Modifier = Modifier,
    vm: EditorViewModel = viewModel(),
) {
    val swatch = NoteColors.at(vm.colorIndex)
    var confirmDelete by remember { mutableStateOf(false) }

    // Guardado automático: al salir de la pantalla y cuando la app pasa a segundo plano.
    DisposableEffect(Unit) { onDispose { vm.saveOnExit() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.saveOnExit() }

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
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
            ) {
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

                if (vm.kind == NoteKind.TASK) {
                    TaskOptions(
                        dueAt = vm.dueAt,
                        completed = vm.completed,
                        swatch = swatch,
                        onCompletedChange = { vm.completed = it },
                        onDueChange = {
                            vm.dueAt = it
                            if (it == null) vm.reminderMinute = null
                        },
                        reminderMinute = vm.reminderMinute,
                        onReminderChange = { vm.reminderMinute = it },
                    )
                }

                NoteField(
                    value = vm.title,
                    onValueChange = { vm.title = it },
                    hint = stringResource(R.string.editor_title_hint),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    swatch = swatch,
                )
                NoteField(
                    value = vm.body,
                    onValueChange = { vm.body = it },
                    hint = stringResource(R.string.editor_body_hint),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    swatch = swatch,
                )

                vm.items.forEach { item ->
                    key(item.id) {
                        ItemRow(item = item, swatch = swatch, onRemove = { vm.removeItem(item) })
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

                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NoteColors.all.forEachIndexed { index, option ->
                        val selected = vm.colorIndex == index
                        val description = stringResource(R.string.cd_color, index + 1)
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(44.dp)
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
private fun ItemRow(item: EditItem, swatch: NoteSwatch, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = { item.checked = it },
            colors = CheckboxDefaults.colors(
                checkedColor = swatch.accent,
                uncheckedColor = swatch.accent,
                checkmarkColor = swatch.background,
            ),
        )
        TextField(
            value = item.text,
            onValueChange = { item.text = it },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            ),
            placeholder = { Text(stringResource(R.string.editor_item_hint)) },
            colors = noteFieldColors(swatch),
        )
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
