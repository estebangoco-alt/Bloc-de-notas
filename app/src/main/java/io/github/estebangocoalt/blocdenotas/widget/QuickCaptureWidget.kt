package io.github.estebangocoalt.blocdenotas.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.capture.QuickCaptureActivity

/**
 * Widget 4×1 de captura rápida. Al tocarlo abre la hoja de captura con el teclado activo.
 */
class QuickCaptureWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content(context) }
    }

    @Composable
    private fun Content(context: Context) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(32.dp)
                .background(ColorProvider(day = Color(0xFFFBF8F3), night = Color(0xFFFBF8F3)))
                .padding(horizontal = 20.dp)
                .clickable(actionStartActivity(Intent(context, QuickCaptureActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_hint),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = ColorProvider(day = Color(0xFF5C554B), night = Color(0xFF5C554B)), fontSize = 16.sp),
            )
            // Micrófono: abre la hoja de captura y empieza a grabar al momento.
            Box(
                modifier = GlanceModifier
                    .size(44.dp)
                    .cornerRadius(22.dp)
                    .background(ColorProvider(day = Color(0xFFDCEBE8), night = Color(0xFFDCEBE8)))
                    .clickable(
                        actionStartActivity(
                            Intent(context, QuickCaptureActivity::class.java)
                                .setAction(QuickCaptureActivity.ACTION_RECORD)
                                .putExtra(QuickCaptureActivity.EXTRA_RECORD, true),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_mic),
                    contentDescription = context.getString(R.string.attach_record),
                    modifier = GlanceModifier.size(22.dp),
                )
            }
            Box(modifier = GlanceModifier.size(8.dp)) {}
            Box(
                modifier = GlanceModifier
                    .size(44.dp)
                    .cornerRadius(22.dp)
                    .background(ColorProvider(day = Color(0xFF0B6E63), night = Color(0xFF0B6E63))),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = GlanceModifier.size(22.dp),
                )
            }
        }
    }
}

class QuickCaptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCaptureWidget()
}
