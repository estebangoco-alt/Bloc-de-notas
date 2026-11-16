package io.github.estebangocoalt.blocdenotas.ui.shopping

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

    Surface(modifier.fillMaxSize(), color = swatch.background, contentColor = swatch.content) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = data?.note?.title.orEmpty().ifBlank { stringResource(R.string.shopping_mode) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = if (total > 0 && done == total) {
                            stringResource(R.string.shopping_all_done)
                        } else {
                            stringResource(R.string.shopping_progress, done, total)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done.toFloat() / total },
                color = swatch.accent,
                trackColor = swatch.content.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (total == 0) {
                Text(
                    text = stringResource(R.string.shopping_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    ShoppingRow(item = item, swatch = swatch, onToggle = { vm.setChecked(item.id, !item.checked) })
                }
            }

            if (done > 0) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = vm::uncheckAll) { Text(stringResource(R.string.shopping_uncheck_all)) }
                    TextButton(onClick = { confirmRemove = true }) { Text(stringResource(R.string.shopping_remove_checked)) }
                }
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
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = if (item.checked) 0.25f else 0.6f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BigCheck(checked = item.checked, swatch = swatch)
        Text(
            text = item.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            color = swatch.content.copy(alpha = if (item.checked) 0.5f else 1f),
            modifier = Modifier.padding(start = 16.dp).weight(1f),
        )
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
