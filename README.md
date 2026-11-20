# Bloc de notas

> Notas, tareas, ideas, lista de la compra, tablero tipo Trello y calendario en una sola app para Android, pensada para apuntar de forma instantánea desde la pantalla de inicio. Software libre, sin anuncios, sin cuentas y sin conexión por defecto (la sincronización con tu propio servidor es opcional).

**Estado: pre-alpha, en desarrollo.** La app ya funciona en Android con casi todas las funciones de la versión 1 (ver abajo); aún falta probarla a fondo, el icono final en las tiendas y publicar la primera versión. Consulta la [hoja de ruta](docs/ROADMAP.md) y el [historial de cambios](CHANGELOG.md).

<!-- Cuando haya capturas, súbelas a fastlane/metadata/android/es-ES/images/phoneScreenshots/ y enlázalas aquí -->

## Por qué existe

Entre las apps libres de F-Droid hay buenas apps de notas y buenas apps de tareas, pero ninguna que junte en una sola, y de forma totalmente local:

- notas, ideas y listas de la compra,
- tareas con fecha y recordatorios,
- tablero kanban,
- calendario,
- widgets de captura instantánea.

Los tableros kanban libres suelen necesitar un servidor propio. Esta app guarda todo en el móvil.

## Características

Las casillas indican el estado actual: ✅ hecho, ⬜ pendiente.

### Pantallas
- **Resumen** (primera pestaña): tarjetas de Notas, Tareas, Tablero y Calendario con sus números, gráfico de tareas de los próximos 7 días, tareas de hoy y atrasadas, avance de cada tablero y notas recientes.
- **Notas**: notas, ideas y listas de la compra con búsqueda, filtros por tipo y etiquetas, vistas guardadas y plantillas.
- **Tareas**: agrupadas en vencidas, hoy, próximos 7 días, más adelante, sin fecha y hechas.
- **Tablero**: kanban estilo Trello con **varios tableros (uno por proyecto)**, columnas editables y tarjetas que se arrastran con pulsación larga.
- **Calendario**: vista de mes y de agenda, con las tareas de cada día.

### MVP
- [x] Widget de captura rápida (4×1) con elección de destino y prioridad
- [ ] Atajos del icono y tile de ajustes rápidos
- [x] Bandeja de entrada para lo capturado sin clasificar
- [x] Compartir texto desde otras apps hacia la bandeja
- [x] Notas de texto y listas con casillas
- [x] Colores, notas fijadas y búsqueda
- [x] Recordatorios con notificación
- [x] Plantillas (compra semanal, reunión, proyecto, diario) y plantillas propias («Guardar como plantilla»)
- [x] Modo compra (filas grandes, pantalla encendida y artículos agrupados por pasillo)
- [x] Copia de seguridad y restauración en ZIP
- [x] Tema claro, oscuro y Material You (con interruptor en Inicio y en Ajustes)
- [x] Funciona sin conexión: el permiso de internet solo se usa en la sincronización opcional, apagada por defecto

### Versión 1
- [x] Tablero kanban con columnas, subtareas y varios tableros por proyecto
- [x] Calendario mensual con agenda
- [x] Tareas con fecha, hora de aviso, prioridad y repetición (diaria, semanal, mensual, anual)
- [x] Vistas guardadas en Notas, Tareas, Tablero y Calendario, también dentro de la copia de seguridad
- [x] Orden (fecha, prioridad, título, recientes) y filtros (prioridad, etiqueta, ocultar hechas) en Tareas, Tablero y Calendario
- [x] Propiedades personalizadas: estado (columna), fecha, prioridad y etiquetas
- [x] Enlaces entre notas `[[Título]]`, menciones `@` («Mencionada en») y subpáginas
- [x] Comandos con `/` en el editor
- [x] Widgets de lista de tareas, agenda y notas fijadas
- [x] Bloqueo con huella, cara o bloqueo de pantalla, y papelera (30 días)
- [x] Exportar a Markdown y a JSON (dentro de la copia de seguridad)
- [x] Resumen (dashboard) con los datos de las cuatro secciones
- [x] Datos de ejemplo para probar la app (Ajustes)
- [x] Castellano e inglés
- [x] Plantillas propias, secciones por pasillo en el modo compra y orden/filtros en Tareas
- [x] Vistas guardadas también en tablero y calendario, y dentro de la copia de seguridad

### Versión 2 (ideas)
- [x] Sincronización opcional con WebDAV / Nextcloud (Syncthing: pendiente)
- [x] Imágenes y notas de voz adjuntas a las notas
- [x] Importar desde Google Keep y Todoist
- [x] Relaciones entre tareas (dependencias) y línea de tiempo

## Widgets

Al añadir un widget en la pantalla de inicio aparecen cuatro, cada uno con su nombre:

| Widget | Para qué sirve |
|---|---|
| **Captura rápida** (4×1) | Escribir una nota al instante; elige destino y, en las tareas, la prioridad |
| **Lista de tareas** (3×3) | Tus tareas pendientes, las que tienen fecha primero |
| **Agenda** (3×3) | Próximas tareas con fecha y las atrasadas |
| **Notas fijadas** (3×3) | Tus notas fijadas, a un toque |

Tocar una fila abre esa nota. Los widgets de lista se actualizan solos cuando cambian las notas.

## Seguridad y papelera

- **Bloqueo** (Ajustes → Seguridad): pide huella, cara o el bloqueo de pantalla al abrir la app o tras 15 s en segundo plano. Con el bloqueo activo, la app no sale en capturas ni en las apps recientes.
- **Papelera**: borrar envía la nota a la papelera durante 30 días; se puede restaurar o eliminar para siempre.

## Exportar e importar

En **Ajustes** puedes:

- **Copia de seguridad (`.zip` con `backup.json`)**: exportar todo y restaurarlo en otro dispositivo (modo *Combinar* o *Reemplazar*). Incluye notas, listas, tableros, columnas, prioridad, etiquetas, subpáginas y papelera. Formato: [`docs/BACKUP_FORMAT.md`](docs/BACKUP_FORMAT.md).
- **Exportar a Markdown**: un `.zip` con un archivo `.md` por nota, en carpetas por tipo y con cabecera YAML (fechas, etiquetas, prioridad).

Sin permisos de almacenamiento. Incluye las vistas guardadas, las plantillas propias y los archivos adjuntos.

## Instalación

Cuando exista una versión publicada podrás instalarla desde:

- **F-Droid** (objetivo principal). Ver [docs/FDROID.md](docs/FDROID.md).
- **APK** en la sección [Releases](../../releases) de este repositorio.

## Compilar desde el código

Requisitos:

- Android Studio reciente o solo las herramientas de línea de comandos
- JDK 17
- Android SDK (compileSdk 35, minSdk 26: Android 8.0 o superior; ver `app/build.gradle.kts`)

```bash
git clone https://github.com/estebangoco-alt/Bloc-de-notas.git
cd bloc-de-notas
./gradlew assembleDebug
```

El APK de depuración queda en `app/build/outputs/apk/debug/`.

## Tecnología

| Área | Elección |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose + Material 3 |
| Navegación | Navigation Compose |
| Datos | Room (SQLite), con migraciones y esquemas exportados |
| Copia de seguridad | kotlinx.serialization (JSON dentro de un ZIP) |
| Widgets | Jetpack Glance |
| Recordatorios | AlarmManager (se reprograman al reiniciar el móvil) |
| Bloqueo | AndroidX Biometric |
| Tests | JUnit sobre la lógica pura (repeticiones, enlaces, tablero, resumen, copia…) |
| Compilación | Gradle con Gradle Wrapper |

Todas las dependencias deben ser software libre, para cumplir la [política de inclusión de F-Droid](https://f-droid.org/docs/Inclusion_Policy). No se usarán Google Play Services, Firebase ni librerías de anuncios o analítica.

## Privacidad

- Sin cuentas, sin anuncios y sin analítica.
- Todos los datos se guardan en el dispositivo.
- El permiso de internet se usa únicamente para la sincronización opcional con tu servidor WebDAV/Nextcloud, que está apagada por defecto. No hay anuncios, analíticas ni servidores nuestros. El usuario y la contraseña se guardan en el almacenamiento privado de la app.
- Permisos: micrófono (solo mientras grabas una nota de voz), internet (solo con la sincronización activada), notificaciones, alarmas exactas (recordatorios), arrancar al iniciar el móvil (reprogramar avisos) y biometría (bloqueo opcional).
- Con el bloqueo activado, la ventana se oculta en capturas y apps recientes.

## Estructura del repositorio

```text
app/                         Código de la app Android (Kotlin + Compose)
docs/                        Hoja de ruta, formato de la copia y guía de publicación
scripts/                     Utilidades (p. ej. cambiar el identificador de la app)
fastlane/metadata/android/   Textos, iconos y capturas para F-Droid
CONTRIBUTING.md              Guía para colaborar
SECURITY.md                  Cómo reportar vulnerabilidades
CHANGELOG.md                 Historial de cambios
LICENSE                      Licencia (GPL-3.0)
```

## Contribuir

Las ideas, los errores y las traducciones son bienvenidos. Lee [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir una incidencia o un pull request.

## Licencia

Software libre bajo la licencia **GNU GPL v3 o posterior**. Consulta el archivo [LICENSE](LICENSE).

© Esteban y colaboradores.
