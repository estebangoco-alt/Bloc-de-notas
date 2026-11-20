# Arquitectura

Una sola app (módulo `app`), sin servidores. Kotlin + Jetpack Compose, con una arquitectura sencilla de capas: **interfaz → ViewModel → repositorio → Room**.

```text
UI (Compose)  →  ViewModel  →  NoteRepository  →  NoteDao (Room / SQLite)
                                      ↑
                  lógica pura (data/*Logic.kt) probada con JUnit
```

## Paquetes (`io.github.estebangocoalt.blocdenotas`)

| Paquete | Qué contiene |
|---|---|
| `data/` | Entidades de Room (`NoteEntity`, `BoardEntity`…), DAO, base de datos con migraciones, repositorio y **toda la lógica pura**: repeticiones, etiquetas, filtros, dependencias, línea de tiempo, pasillos, cantidades, festivos… Sin Android, para probarla con JUnit |
| `ui/` | Pantallas en Compose: `home`, `notes`, `tasks`, `board`, `calendar`, `dashboard`, `editor`, `settings`, `common` y el tema |
| `widget/` | Widgets de Jetpack Glance (captura rápida, tareas, agenda, notas fijadas) |
| `capture/` | Hoja de captura rápida y recepción de texto compartido |
| `reminders/` | Alarmas de recordatorio y reprogramación al arrancar |
| `backup/` | Copia de seguridad ZIP (`backup.json` + adjuntos) y exportación a Markdown |
| `importer/` | Importadores de Google Keep (Takeout) y Todoist (CSV) |
| `sync/` | Sincronización opcional con WebDAV (cliente propio con ETag) |
| `attachments/` | Imágenes y notas de voz en el almacenamiento privado |
| `lock/` | Bloqueo con biometría |

## Decisiones de diseño

- **Ids UUID (texto)**: permiten combinar copias de seguridad de varios dispositivos sin colisiones. Los importadores usan ids derivados del contenido para no duplicar al repetir.
- **Una tarea es una nota** (`NoteEntity` con `kind = TASK`): el tablero, la lista de tareas y el calendario son tres vistas de los mismos datos.
- **Lógica pura aparte de Android**: así las reglas importantes (repeticiones, dependencias, importadores, fusión de copias) se prueban rápido con `./gradlew test`.
- **Fechas** de tareas: `dueAt` es la medianoche UTC del día elegido (ver `TaskLogic`), para que no cambien con la zona horaria.
- **Papelera**: borrado lógico (`deletedAt`), se vacía a los 30 días.
- **Copia de seguridad**: JSON versionado dentro de un ZIP; los campos nuevos son opcionales para no romper copias antiguas ([formato](BACKUP_FORMAT.md)).
- **Sincronización**: sube y baja una copia completa a una URL WebDAV y fusiona por nota más reciente. No propaga borrados definitivos.
- **Privacidad**: sin dependencias de Google, sin analítica; internet solo para la sincronización opcional.

## Base de datos

Room con esquemas exportados en `app/schemas/`. Cada cambio de esquema necesita una migración (`AppDatabase.kt`) y subir la versión. Hay que subir a Git el nuevo `N.json` que genera la compilación.

## Pruebas

`./gradlew test` ejecuta las pruebas de lógica en `app/src/test`. No hay aún pruebas de interfaz.
