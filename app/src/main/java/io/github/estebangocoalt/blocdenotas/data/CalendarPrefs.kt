package io.github.estebangocoalt.blocdenotas.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** Preferencias del calendario: zona de festivos y si se muestra el santoral. Se guardan en SharedPreferences. */
class CalendarPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("calendar", Context.MODE_PRIVATE)
    private val locale = Locale.getDefault()

    private val _region = MutableStateFlow(
        runCatching { HolidayRegion.valueOf(prefs.getString(KEY_REGION, null) ?: "") }
            // Primera vez: festivos de España si el móvil está en España; si no, ninguno.
            .getOrDefault(if (locale.country == "ES") HolidayRegion.SPAIN else HolidayRegion.NONE),
    )
    val region: StateFlow<HolidayRegion> = _region

    private val _saints = MutableStateFlow(
        // Primera vez: santoral activado si el idioma es castellano o catalán.
        if (prefs.contains(KEY_SAINTS)) prefs.getBoolean(KEY_SAINTS, false) else locale.language in listOf("es", "ca"),
    )
    val showSaints: StateFlow<Boolean> = _saints

    fun setRegion(value: HolidayRegion) {
        _region.value = value
        prefs.edit().putString(KEY_REGION, value.name).apply()
    }

    fun setShowSaints(value: Boolean) {
        _saints.value = value
        prefs.edit().putBoolean(KEY_SAINTS, value).apply()
    }

    private companion object {
        const val KEY_REGION = "region"
        const val KEY_SAINTS = "saints"
    }
}
