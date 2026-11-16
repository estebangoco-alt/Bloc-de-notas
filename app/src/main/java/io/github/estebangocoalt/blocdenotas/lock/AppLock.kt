package io.github.estebangocoalt.blocdenotas.lock

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Bloqueo de la app con huella, cara o el bloqueo de pantalla del móvil.
 *
 * La app se bloquea al abrirla y cuando ha estado más de [GRACE_MS] en segundo plano
 * (así copiar algo de otra app no obliga a desbloquear cada vez).
 */
class AppLock(context: Context) {
    private val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled

    /** Empieza bloqueada si el ajuste está activo. */
    private val _locked = MutableStateFlow(_enabled.value)
    val locked: StateFlow<Boolean> = _locked

    private var backgroundAt = 0L

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _locked.value = false
    }

    fun unlock() {
        _locked.value = false
    }

    fun onBackground(now: Long = System.currentTimeMillis()) {
        backgroundAt = now
    }

    fun onForeground(now: Long = System.currentTimeMillis()) {
        if (_enabled.value && (backgroundAt == 0L || now - backgroundAt > GRACE_MS)) _locked.value = true
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        const val GRACE_MS = 15_000L

        /** Huella o cara; en Android 11 o superior también el PIN, patrón o contraseña del móvil. */
        val AUTHENTICATORS: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_WEAK or DEVICE_CREDENTIAL else BIOMETRIC_WEAK

        fun isAvailable(context: Context): Boolean =
            BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
