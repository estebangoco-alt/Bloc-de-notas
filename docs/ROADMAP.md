# Hoja de ruta

Las fases se pueden reordenar según lo que aprendamos. Nada de esto está implementado todavía.

## Fase 0: diseño (actual)
- [x] Análisis de apps similares
- [x] Lista de funciones por versión
- [x] Mockups de 12 pantallas
- [ ] Decidir nombre definitivo, icono y paleta
- [ ] Elegir ID de aplicación (por ejemplo `io.github.estebangocoalt.blocdenotas`)

## Fase 1: base técnica
- [ ] Proyecto Android con Kotlin, Compose y Material 3
- [ ] Base de datos Room con notas, tareas, etiquetas y propiedades
- [ ] Navegación: Notas, Tareas, Tablero, Calendario
- [ ] Tema claro, oscuro y Material You
- [ ] Integración continua que compile y pase las pruebas

## Fase 2: MVP (v0.1)
- [ ] Notas y listas con casillas, colores y fijar
- [ ] Búsqueda
- [ ] Bandeja de entrada y captura rápida
- [ ] Compartir desde otras apps
- [ ] Widget de captura rápida 4×1
- [ ] Atajos del icono y tile de ajustes rápidos (idea pendiente)
- [ ] Recordatorios
- [ ] Plantillas y modo compra
- [ ] Copia de seguridad ZIP
- [ ] Ficha de F-Droid lista (textos, icono, capturas)
- [ ] Envío a F-Droid (ver [FDROID.md](FDROID.md))

## Fase 3: versión 1.0
- [ ] Tablero kanban con subtareas
- [ ] Calendario y agenda
- [ ] Tareas con repetición
- [x] Vistas guardadas (Notas, Tareas, Tablero y Calendario) y propiedades personalizadas
- [x] Plantillas propias, pasillos en el modo compra y orden/filtros en Tareas
- [x] Enlaces entre notas, menciones y subpáginas
- [x] Comandos con `/`
- [x] Widgets de lista, agenda y notas fijadas
- [x] Bloqueo con huella y papelera
- [x] Exportar a Markdown y JSON

## Fase 4: ideas para después
- [x] Sincronización opcional con WebDAV / Nextcloud
- [ ] Sincronización por carpeta (Syncthing)
- [x] Imágenes y notas de voz
- [x] Importar desde Google Keep y Todoist
- [x] Relaciones entre tareas (dependencias) y línea de tiempo
- [ ] Compilaciones reproducibles

## Fuera de alcance
Colaboración en tiempo real, inteligencia artificial integrada y cuentas en la nube. Chocan con el enfoque local y con las reglas de F-Droid.
