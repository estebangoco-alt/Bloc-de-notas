package io.github.estebangocoalt.blocdenotas.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.TagLogic

/** Etiquetas de la nota: chips con «×» para quitarlas y «+ Etiqueta» para añadir (con sugerencias). */
@Composable
fun TagEditor(
    tags: List<String>,
    known: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = { onRemove(tag) },
                label = { Text("#$tag") },
                trailingIcon = {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.tag_remove, tag))
                },
            )
        }
        AssistChip(
            onClick = { dialog = true },
            label = { Text(stringResource(R.string.tag_add)) },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
        )
    }

    if (dialog) {
        var text by remember { mutableStateOf("") }
        val suggestions = known.filter { k -> tags.none { it.equals(k, ignoreCase = true) } }
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text(stringResource(R.string.tag_add)) },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(R.string.tag_hint)) },
                        singleLine = true,
                    )
                    if (suggestions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            suggestions.forEach { tag ->
                                SuggestionChip(
                                    onClick = {
                                        onAdd(tag)
                                        dialog = false
                                    },
                                    label = { Text("#$tag") },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = TagLogic.normalize(text).isNotEmpty(),
                    onClick = {
                        onAdd(text)
                        dialog = false
                    },
                ) { Text(stringResource(R.string.action_add)) }
            },
            dismissButton = {
                TextButton(onClick = { dialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
