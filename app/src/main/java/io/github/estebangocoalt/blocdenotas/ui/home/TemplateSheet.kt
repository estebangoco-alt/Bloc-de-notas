package io.github.estebangocoalt.blocdenotas.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.ui.common.labelRes
import io.github.estebangocoalt.blocdenotas.ui.editor.NoteTemplate
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors

private fun NoteTemplate.colorIndex(): Int = when (this) {
    NoteTemplate.WEEKLY_SHOPPING -> 1
    NoteTemplate.MEETING -> 0
    NoteTemplate.PROJECT -> 4
    NoteTemplate.DIARY -> 3
}

/** Galería «Nueva desde plantilla»: cuadrícula de 2 columnas con colores pastel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSheet(
    onPick: (NoteTemplate) -> Unit,
    onBlank: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.template_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            val tiles: List<Triple<Int, Int, () -> Unit>> =
                NoteTemplate.entries.map { t ->
                    Triple(t.colorIndex(), t.label) { onPick(t) }
                } + Triple(5, R.string.template_blank, onBlank)
            tiles.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (color, label, action) ->
                        val swatch = NoteColors.at(color)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(92.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(swatch.background)
                                .clickable(onClick = action)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(
                                text = stringResource(label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = swatch.content,
                            )
                        }
                    }
                    if (row.size == 1) Column(Modifier.weight(1f)) {}
                }
            }
        }
    }
}
