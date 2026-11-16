package io.github.estebangocoalt.blocdenotas.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.ChecklistItemEntity
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteSwatch

@StringRes
fun NoteKind.labelRes(): Int = when (this) {
    NoteKind.NOTE -> R.string.kind_note
    NoteKind.IDEA -> R.string.kind_idea
    NoteKind.SHOPPING -> R.string.kind_shopping
    NoteKind.TASK -> R.string.kind_task
}

/** Casilla pequeña de solo lectura para las tarjetas. */
@Composable
fun MiniCheck(checked: Boolean, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(shape)
            .then(
                if (checked) Modifier.background(color) else Modifier.border(2.dp, color, shape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun MiniChecklistRow(item: ChecklistItemEntity, swatch: NoteSwatch) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniCheck(checked = item.checked, color = swatch.accent)
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Pantalla provisional para las secciones que llegan en versiones posteriores. */
@Composable
fun ComingSoonScreen(@StringRes titleRes: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.coming_soon_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = stringResource(R.string.coming_soon_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
