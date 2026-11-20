package io.github.estebangocoalt.blocdenotas.ui.shopping

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import io.github.estebangocoalt.blocdenotas.data.AisleLogic
import io.github.estebangocoalt.blocdenotas.ui.common.labelRes
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteSwatch

/**
 * Modo compra: filas grandes para tocar con el carro en la mano y la pantalla siempre encendida.
 * Los artículos marcados bajan al final de la lista.
 */
@Composable
fun ShoppingModeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: ShoppingViewModel = viewModel(),
) {
    val data by vm.note.collectAsStateWithLifecycle()
    var confirmRemove by remember { mutableStateOf(false) }

    // Pantalla encendida mientras se está en este modo.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val swatch = NoteColors.at(data?.note?.colorIndex ?: 1)
    val items = data?.items.orEmpty().sortedWith(compareBy<ChecklistItemEntity> { it.checked }.thenBy { it.position })
    val total = items.size
    val done = items.count { it.checked }
    var byAisle by rememberSaveable { mutableStateOf(true) }
    val groups = remember(items) { AisleLogic.group(items) }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(swatch.background, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, bottom = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = swatch.content,
                        )
                    }
                    Text(
                        text = data?.note?.title.orEmpty().ifBlank { stringResource(R.string.shopping_mode) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = swatch.content,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.shopping_screen_on),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = swatch.content,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp)) {
                    Text(
                        text = if (total > 0 && done == total) {
                            stringResource(R.string.shopping_all_done)
                        } else {
                            stringResource(R.string.shopping_progress, done, total)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = swatch.content,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${if (total == 0) 0 else done * 100 / total} %",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = swatch.content,
                    )
                }
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done.toFloat() / total },
                    color = swatch.accent,
                    trackColor = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp).height(8.dp).clip(CircleShape),
                )
            }

            if (total == 0) {
                Text(
                    text = stringResource(R.string.shopping_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
            }

            if (total > 1) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(true to R.string.shopping_by_aisle, false to R.string.shopping_by_list).forEach { (value, label) ->
                        FilterChip(
                            selected = byAisle == value,
                            onClick = { byAisle = value },
                            label = { Text(stringResource(label)) },
                            shape = CircleShape,
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = swatch.accent,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                if (byAisle) {
                    groups.forEach { (aisle, list) ->
                        item(key = "aisle-${aisle.name}") {
                            val left = list.count { !it.checked }
                            Text(
                                text = "${stringResource(aisle.labelRes())} · $left/${list.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = swatch.accent,
                                modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
                            )
                        }
                        items(list, key = { it.id }) { item ->
                            ShoppingRow(item = item, swatch = swatch, onToggle = { vm.setChecked(item.id, !item.checked) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        }
                    }
                } else {
                    items(items, key = { it.id }) { item ->
                        ShoppingRow(item = item, swatch = swatch, onToggle = { vm.setChecked(item.id, !item.checked) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }

            if (done > 0) {
                TextButton(onClick = vm::uncheckAll, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.shopping_uncheck_all))
                }
            }
            Button(
                onClick = { if (done > 0) confirmRemove = true else onBack() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(56.dp),
            ) {
                Text(
                    text = stringResource(R.string.shopping_finish),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.shopping_remove_title)) },
            text = { Text(stringResource(R.string.shopping_remove_text)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.removeChecked()
                    confirmRemove = false
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun ShoppingRow(item: ChecklistItemEntity, swatch: NoteSwatch, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BigCheck(checked = item.checked, swatch = swatch)
        val entry = remember(item.text) { io.github.estebangocoalt.blocdenotas.data.QuantityLogic.parse(item.text) }
        Text(
            text = entry.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            color = if (item.checked) swatch.accent.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp).weight(1f),
        )
        if (entry.quantity != null) {
            Text(
                text = entry.quantity,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = swatch.accent,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .background(swatch.accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun BigCheck(checked: Boolean, swatch: NoteSwatch) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .then(
                if (checked) Modifier.background(swatch.accent) else Modifier.border(3.dp, swatch.accent, shape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}
