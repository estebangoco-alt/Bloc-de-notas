package io.github.estebangocoalt.blocdenotas.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.estebangocoalt.blocdenotas.blocApp
import kotlinx.coroutines.launch

/** Android borra las alarmas al reiniciar: aquí se vuelven a programar los avisos pendientes. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val repository = context.blocApp.repository
        val pending = goAsync()
        repository.appScope.launch {
            try {
                repository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
