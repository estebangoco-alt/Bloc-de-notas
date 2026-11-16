# Bloc de notas

> Notas, tareas, ideas, lista de la compra, tablero tipo Trello y calendario en una sola app para Android, pensada para apuntar de forma instantánea desde la pantalla de inicio. Software libre, sin anuncios, sin cuentas y 100 % sin conexión.

**Estado: fase de diseño (pre-alpha).** Ya existen los mockups de 12 pantallas; el código de la app todavía no está publicado. Consulta la [hoja de ruta](docs/ROADMAP.md).

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

Las casillas indican el estado actual. Todo está pendiente por ahora.

### MVP
- [ ] Widget de captura rápida (4×1), atajos del icono y tile de ajustes rápidos
- [ ] Bandeja de entrada para lo capturado sin clasificar
- [ ] Compartir texto desde otras apps hacia la bandeja
- [ ] Notas de texto y listas con casillas
- [ ] Colores, notas fijadas y búsqueda
- [ ] Recordatorios
- [ ] Plantillas (compra semanal, reunión, proyecto, diario)
- [ ] Modo compra (filas grandes y pantalla encendida)
- [ ] Copia de seguridad y restauración en ZIP
- [ ] Tema claro, oscuro y Material You
- [ ] Funciona sin permiso de internet

### Versión 1
- [ ] Tablero kanban con columnas y subtareas
- [ ] Calendario mensual con agenda
- [ ] Tareas con fecha y repetición
- [ ] Vistas guardadas (filtro, orden y agrupación) en lista, tablero y calendario
- [ ] Propiedades personalizadas (estado, fecha, prioridad, etiquetas)
- [ ] Enlaces entre notas `[[ ]]`, menciones y subpáginas
- [ ] Comandos con `/` en el editor
- [ ] Widgets de lista, agenda y notas fijadas
- [ ] Bloqueo con huella, papelera
- [ ] Exportar a Markdown y JSON

### Versión 2 (ideas)
- [ ] Sincronización opcional (WebDAV / Nextcloud / Syncthing)
- [ ] Nota de voz e imágenes
- [ ] Importar desde Google Keep y Todoist
- [ ] Relaciones entre tablas y línea de tiempo

## Exportar e importar

En **Ajustes** puedes exportar todo a un `.zip` y restaurarlo en otro dispositivo (modo *Combinar* o *Reemplazar*). Sin permisos de almacenamiento ni internet. Formato: [`docs/BACKUP_FORMAT.md`](docs/BACKUP_FORMAT.md).

## Instalación

Cuando exista una versión publicada podrás instalarla desde:

- **F-Droid** (objetivo principal). Ver [docs/FDROID.md](docs/FDROID.md).
- **APK** en la sección [Releases](../../releases) de este repositorio.

## Compilar desde el código

Requisitos previstos:

- Android Studio reciente o solo las herramientas de línea de comandos
- JDK 17
- Android SDK (la versión exacta se indicará en `app/build.gradle.kts`)

```bash
git clone https://github.com/estebangoco-alt/Bloc-de-notas.git
cd bloc-de-notas
./gradlew assembleDebug
```

El APK de depuración queda en `app/build/outputs/apk/debug/`.

## Tecnología prevista

| Área | Elección |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose + Material 3 |
| Datos | Room (SQLite) |
| Widgets | Jetpack Glance |
| Recordatorios | WorkManager / AlarmManager |
| Compilación | Gradle con Gradle Wrapper |

Todas las dependencias deben ser software libre, para cumplir la [política de inclusión de F-Droid](https://f-droid.org/docs/Inclusion_Policy). No se usarán Google Play Services, Firebase ni librerías de anuncios o analítica.

## Privacidad

- Sin cuentas, sin anuncios y sin analítica.
- Todos los datos se guardan en el dispositivo.
- La app no pedirá el permiso de internet en el MVP.
- Permisos previstos: notificaciones, alarmas exactas (recordatorios) y biometría (bloqueo opcional).

## Estructura prevista del repositorio

```text
app/                         Código de la app Android
docs/                        Hoja de ruta y guía de publicación
fastlane/metadata/android/   Textos, iconos y capturas para F-Droid
CONTRIBUTING.md              Guía para colaborar
CHANGELOG.md                 Historial de cambios
LICENSE                      Licencia (GPL-3.0)
```

## Contribuir

Las ideas, los errores y las traducciones son bienvenidos. Lee [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir una incidencia o un pull request.

## Licencia

Software libre bajo la licencia **GNU GPL v3 o posterior**. Consulta el archivo [LICENSE](LICENSE).

© Esteban y colaboradores.
