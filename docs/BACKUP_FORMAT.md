# Formato de copia de seguridad

Bloc de notas exporta todo su contenido a un único archivo `.zip` que se puede
importar en otro dispositivo. Este documento describe el formato (versión 1).

## Contenedor

- Archivo ZIP con **una sola entrada**: `backup.json` (UTF-8).
- Al importar también se acepta un `.json` plano con el mismo contenido.
- No está cifrado. Guárdalo en un lugar de confianza.
- Límite de lectura: 64 MB. Se lee solo la entrada con nombre exacto
  `backup.json` (nunca se extraen rutas del ZIP al disco).

## JSON

```json
{
  "format": "bloc-de-notas-backup",
  "version": 1,
  "exportedAt": 1731670000000,
  "appVersion": "0.1.0",
  "columns": [
    { "id": "uuid", "title": "Por hacer", "position": 0 }
  ],
  "notes": [
    {
      "id": "uuid",
      "title": "Compra",
      "body": "",
      "kind": "SHOPPING",
      "colorIndex": 1,
      "pinned": false,
      "inbox": false,
      "createdAt": 1731600000000,
      "updatedAt": 1731660000000,
      "dueAt": 1731628800000,
      "completedAt": null,
      "reminderMinute": 540,
      "repeat": "WEEKLY",
      "priority": 3,
      "tags": ["casa", "curso"],
      "parentId": "uuid-de-la-nota-madre",
      "deletedAt": null,
      "columnId": "uuid-de-columna",
      "columnPosition": 0,
      "items": [
        { "id": "uuid", "text": "leche", "checked": false, "position": 0 }
      ]
    }
  ]
}
```

`kind`: `NOTE`, `TASK`, `IDEA`, `SHOPPING`. Las fechas son milisegundos Unix.

`repeat` indica si la tarea se repite: `DAILY`, `WEEKLY`, `MONTHLY` o `YEARLY` (ausente o `null` = no se repite). Al marcarla como hecha pasa a su siguiente fecha.

`priority` es la prioridad de la tarea (0 = ninguna, 1 = baja, 2 = media, 3 = alta) y `tags` la lista de etiquetas; ambos son opcionales.

`parentId` es el id de la nota que contiene esta subpágina (ausente o `null` = nota de primer nivel) y `deletedAt` el instante en que se envió a la papelera (`null` = no está en la papelera); ambos son opcionales.

`attachments` lista las imágenes y notas de voz (`id`, `noteId`, `kind` = `IMAGE` | `AUDIO`, `fileName`, `position`, `durationMs`, `createdAt`). Los archivos van en la carpeta `attachments/` del ZIP con ese `fileName` (solo letras, números, `.`, `_` y `-`; los demás nombres se ignoran). Es opcional: las copias anteriores se importan igual, y un adjunto cuyo archivo falta se descarta.

`reminderMinute` es la hora del aviso en minutos desde las 00:00 (540 = 09:00, hora local del día límite).

`dueAt` es la medianoche UTC del día límite de una tarea y `completedAt` el instante en que se marcó como hecha (`null` = pendiente).

`columns`, `columnId` y `columnPosition` describen el tablero (solo las tareas lo usan). Son opcionales: las copias anteriores sin ellos se importan igual y sus tareas caen en la primera columna.

`boards` es la lista de tableros (uno por proyecto: `id`, `title`, `position`) y cada columna indica su tablero en `boardId`. Son opcionales: sin ellos (o con `boardId` vacío o desconocido) las columnas pertenecen al primer tablero.

`views` son las vistas guardadas (`id`, `name`, `scope` = `NOTES` | `TASKS` | `BOARD` | `CALENDAR`, `filter`, `query`, `tasks` con `sort`, `minPriority`, `tag` y `hideDone`, `boardId`, `agenda`) y `templates` las plantillas propias (`id`, `name`, `kind`, `title`, `body`, `items` como lista de textos, `colorIndex`). Cada elemento de lista puede llevar `aisle` (`PRODUCE`, `BAKERY`, `MEAT`, `FISH`, `DAIRY`, `PANTRY`, `DRINKS`, `FROZEN`, `CLEANING`, `HYGIENE` u `OTHER`; vacío = se deduce del nombre). Todos son opcionales: las copias anteriores se importan igual. Una copia sin `views` o `templates` no borra las que ya hay, ni siquiera al reemplazar.

## Reglas de importación

| Modo | Comportamiento |
|------|----------------|
| Combinar | Añade las notas nuevas; actualiza una existente solo si la copia es más reciente (`updatedAt`); nunca borra. |
| Reemplazar | Borra todo y carga el contenido de la copia. |

- Los campos desconocidos se ignoran (compatibilidad hacia delante).
- Un `format` distinto → "no es una copia". Una `version` mayor que la soportada → se rechaza.
- Notas sin `id` o repetidas se descartan; `kind` o `colorIndex` inválidos se corrigen.

## Compatibilidad

Los cambios que añaden campos opcionales mantienen `version: 1`. Cambios
incompatibles subirán la versión y la app seguirá leyendo las anteriores.
