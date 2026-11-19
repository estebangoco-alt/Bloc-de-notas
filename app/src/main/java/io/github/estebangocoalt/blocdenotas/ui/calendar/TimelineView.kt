package io.github.estebangocoalt.blocdenotas.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.NoteEntity
import io.github.estebangocoalt.blocdenotas.data.TimelineLogic
import io.github.estebangocoalt.blocdenotas.ui.theme.NoteColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val DayWidth = 36.dp
private val RowHeight = 54.dp
private val HeaderHeight = 48.dp

/**
 * Línea de tiempo: una barra por tarea con fecha, desde que puede empezar (el día siguiente a sus
 * dependencias, o su creación) hasta su fecha límite. Desliza en horizontal y en vertical.
 */
@Composable
internal fun TimelineView(
    tasks: List<NoteEntity>,
    todayDay: Long,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeline = remember(tasks, todayDay) { TimelineLogic.build(tasks, todayDay) }
    val locale = Locale.getDefault()

    if (timeline.rows.isEmpty()) {
        Text(
            text = stringResource(R.string.timeline_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(20.dp),
        )
        return
    }

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
        ) {
            Column(Modifier.width(DayWidth * timeline.totalDays + 160.dp)) {
                // Cabecera: número del día, y el mes en el primer día de cada mes.
                Row(Modifier.height(HeaderHeight)) {
                    for (i in 0 until timeline.totalDays) {
                        val date = LocalDate.ofEpochDay(timeline.firstDay + i)
                        val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                        val isToday = timeline.firstDay + i == todayDay
                        Column(
                            Modifier
                                .width(DayWidth)
                                .fillMaxHeight()
                                .background(if (weekend) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.Transparent),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (date.dayOfMonth == 1 || i == 0) date.month.getDisplayName(TextStyle.SHORT, locale) else "",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                timeline.rows.forEach { row ->
                    val swatch = NoteColors.at(row.colorIndex)
                    Box(Modifier.fillMaxWidth().height(RowHeight)) {
                        // Línea de hoy.
                        Box(
                            Modifier
                                .offset(x = DayWidth * (todayDay - timeline.firstDay).toInt() + DayWidth / 2 - 1.dp)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        )
                        val barX = DayWidth * (row.startDay - timeline.firstDay).toInt()
                        val barW = DayWidth * row.days - 4.dp
                        Box(
                            Modifier
                                .offset(x = barX, y = 10.dp)
                                .width(barW)
                                .height(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(swatch.accent.copy(alpha = if (row.done) 0.35f else 1f))
                                .then(
                                    if (row.blocked) Modifier.border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(11.dp)) else Modifier,
                                )
                                .clickable { onOpenTask(row.id) },
                        )
                        // El título va a la derecha de la barra para que se lea aunque la barra sea corta.
                        Column(
                            Modifier
                                .offset(x = barX + barW + 6.dp)
                                .width(150.dp)
                                .clickable { onOpenTask(row.id) },
                        ) {
                            Text(
                                text = (if (row.done) "✓ " else "") + row.title.ifBlank { stringResource(R.string.board_untitled) },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (row.dependsOn.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.dep_after, row.dependsOn.joinToString(", ")),
                                    fontSize = 11.sp,
                                    color = if (row.blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (timeline.undated > 0) {
            Text(
                text = stringResource(R.string.timeline_undated, timeline.undated),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}
