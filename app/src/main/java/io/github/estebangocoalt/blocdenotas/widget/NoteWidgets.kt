package io.github.estebangocoalt.blocdenotas.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.estebangocoalt.blocdenotas.MainActivity
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.blocApp
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.data.NoteWithItems
import io.github.estebangocoalt.blocdenotas.data.TaskLogic
import io.github.estebangocoalt.blocdenotas.reminders.ReminderIntents
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Una fila de un widget: texto principal, texto pequeño opcional y la nota que abre. */
internal data class WidgetRow(val noteId: String, val title: String, val hint: String? = null)

private val CARD = ColorProvider(day = Color(0xFFFBF8F3), night = Color(0xFFFBF8F3))
private val INK = ColorProvider(day = Color(0xFF1F1B16), night = Color(0xFF1F1B16))
private val MUTED = ColorProvider(day = Color(0xFF6B6459), night = Color(0xFF6B6459))
private val TEAL = ColorProvider(day = Color(0xFF0B6E63), night = Color(0xFF0B6E63))

private fun openNote(context: Context, id: String): Action =
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .putExtra(ReminderIntents.EXTRA_NOTE_ID, id)
            // Un data distinto por nota evita que Android reutilice el PendingIntent de otra.
            .setData(Uri.parse("bloc://note/$id")),
    )

private fun openApp(context: Context): Action =
    actionStartActivity(Intent(context, MainActivity::class.java).setData(Uri.parse("bloc://home")))

private fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

private fun NoteWithItems.titleOrFallback(context: Context): String =
    note.title.ifBlank { note.body.lineSequence().firstOrNull { it.isNotBlank() } ?: context.getString(R.string.board_untitled) }

/** Base común: tarjeta redondeada con título, filas y un texto cuando no hay nada. */
internal abstract class ListWidget(private val titleRes: Int, private val emptyRes: Int) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    abstract suspend fun rows(context: Context): List<WidgetRow>

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rows = rows(context)
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(CARD).padding(16.dp),
            ) {
                Text(
                    text = context.getString(titleRes),
                    modifier = GlanceModifier.fillMaxWidth().clickable(openApp(context)),
                    style = TextStyle(color = TEAL, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                )
                if (rows.isEmpty()) {
                    Text(
                        text = context.getString(emptyRes),
                        modifier = GlanceModifier.padding(top = 12.dp),
                        style = TextStyle(color = MUTED, fontSize = 14.sp),
                    )
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize().padding(top = 6.dp)) {
                        items(rows) { row -> RowItem(context, row) }
                    }
                }
            }
        }
    }

    @Composable
    private fun RowItem(context: Context, row: WidgetRow) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp).clickable(openNote(context, row.noteId)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.title,
                maxLines = 2,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = INK, fontSize = 14.sp),
            )
            if (row.hint != null) {
                Text(
                    text = row.hint,
                    modifier = GlanceModifier.padding(start = 8.dp),
                    style = TextStyle(color = MUTED, fontSize = 12.sp),
                )
            }
        }
    }
}

private fun dayLabel(context: Context, dueAt: Long): String {
    val diff = TaskLogic.dayOf(dueAt) - todayEpochDay()
    return when {
        diff == 0L -> context.getString(R.string.widget_today)
        diff == 1L -> context.getString(R.string.widget_tomorrow)
        diff < 0L -> context.getString(R.string.widget_overdue)
        else -> LocalDate.ofEpochDay(TaskLogic.dayOf(dueAt)).let { "%02d/%02d".format(it.dayOfMonth, it.monthValue) }
    }
}

/** Tareas pendientes: primero las que tienen fecha (la más cercana arriba). */
internal class TaskListWidget : ListWidget(R.string.widget_tasks_title, R.string.widget_tasks_empty) {
    override suspend fun rows(context: Context): List<WidgetRow> =
        context.blocApp.repository.notes.first()
            .filter { it.note.kind == NoteKind.TASK && it.note.completedAt == null && !it.note.inbox }
            .sortedWith(compareBy({ it.note.dueAt ?: Long.MAX_VALUE }, { -it.note.updatedAt }))
            .take(30)
            .map { WidgetRow(it.note.id, it.titleOrFallback(context), it.note.dueAt?.let { d -> dayLabel(context, d) }) }
}

/** Agenda: tareas pendientes con fecha, de hoy en adelante, más las atrasadas. */
internal class AgendaWidget : ListWidget(R.string.widget_agenda_title, R.string.widget_agenda_empty) {
    override suspend fun rows(context: Context): List<WidgetRow> =
        context.blocApp.repository.notes.first()
            .filter { it.note.dueAt != null && it.note.completedAt == null && !it.note.inbox }
            .sortedBy { it.note.dueAt }
            .take(30)
            .map { WidgetRow(it.note.id, it.titleOrFallback(context), dayLabel(context, it.note.dueAt!!)) }
}

/** Notas fijadas. */
internal class PinnedWidget : ListWidget(R.string.widget_pinned_title, R.string.widget_pinned_empty) {
    override suspend fun rows(context: Context): List<WidgetRow> =
        context.blocApp.repository.notes.first()
            .filter { it.note.pinned && !it.note.inbox }
            .sortedByDescending { it.note.updatedAt }
            .take(30)
            .map { WidgetRow(it.note.id, it.titleOrFallback(context)) }
}

class TaskListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskListWidget()
}

class AgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}

class PinnedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PinnedWidget()
}
