package io.github.estebangocoalt.blocdenotas.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Preferencia de tema (claro/oscuro/sistema y colores dinámicos). Se guarda en SharedPreferences. */
class ThemePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_MODE, null) ?: "") }.getOrDefault(ThemeMode.SYSTEM),
    )
    val mode: StateFlow<ThemeMode> = _mode

    private val _dynamic = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, false))
    val dynamic: StateFlow<Boolean> = _dynamic

    fun setMode(value: ThemeMode) {
        _mode.value = value
        prefs.edit().putString(KEY_MODE, value.name).apply()
    }

    fun setDynamic(value: Boolean) {
        _dynamic.value = value
        prefs.edit().putBoolean(KEY_DYNAMIC, value).apply()
    }

    /** Cambia entre claro y oscuro según cómo se está viendo ahora. */
    fun toggle(currentlyDark: Boolean) = setMode(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_DYNAMIC = "dynamic"
    }
}
