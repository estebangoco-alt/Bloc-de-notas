package io.github.estebangocoalt.blocdenotas

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.github.estebangocoalt.blocdenotas.lock.AppLock
import io.github.estebangocoalt.blocdenotas.lock.LockScreen
import io.github.estebangocoalt.blocdenotas.reminders.ReminderIntents
import io.github.estebangocoalt.blocdenotas.ui.BlocNavHost
import io.github.estebangocoalt.blocdenotas.ui.theme.BlocTheme

// FragmentActivity (en vez de ComponentActivity) porque el aviso de huella lo necesita.
class MainActivity : FragmentActivity() {
    /** Tarea que hay que abrir porque el usuario tocó su aviso. */
    private val noteToOpen = mutableStateOf<String?>(null)
    private var prompting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        noteToOpen.value = intent?.getStringExtra(ReminderIntents.EXTRA_NOTE_ID)
        val lock = blocApp.appLock
        setContent {
            BlocTheme {
                val locked by lock.locked.collectAsState()
                val lockEnabled by lock.enabled.collectAsState()

                // Con el bloqueo activo, la ventana no sale en capturas ni en las apps recientes.
                LaunchedEffect(lockEnabled) {
                    if (lockEnabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    BlocNavHost(
                        openNoteId = noteToOpen.value,
                        onOpenNoteHandled = {
                            noteToOpen.value = null
                            intent?.removeExtra(ReminderIntents.EXTRA_NOTE_ID)
                        },
                    )
                    if (locked) LockScreen(onUnlock = ::authenticate)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val lock = blocApp.appLock
        lock.onForeground()
        if (lock.locked.value) authenticate()
    }

    override fun onStop() {
        super.onStop()
        // Girar la pantalla también recrea la actividad: eso no cuenta como salir de la app.
        if (!isChangingConfigurations) blocApp.appLock.onBackground()
    }

    private fun authenticate() {
        if (prompting || !blocApp.appLock.locked.value) return
        prompting = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    prompting = false
                    blocApp.appLock.unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Cancelado o con error: sigue bloqueada y se puede reintentar con el botón.
                    prompting = false
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_prompt_title))
            .setAllowedAuthenticators(AppLock.AUTHENTICATORS)
            .apply {
                // Sin PIN de respaldo (Android 10 o anterior) hay que ofrecer un botón de cancelar.
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                    setNegativeButtonText(getString(R.string.action_cancel))
                }
            }
            .build()
        prompt.authenticate(info)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        noteToOpen.value = intent.getStringExtra(ReminderIntents.EXTRA_NOTE_ID)
    }
}
