# Historial de cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado semántico.

## [Sin publicar]

### Añadido
- Calendario: deslizar a izquierda o derecha para cambiar de mes; días festivos en rojo (España o Cataluña, calculados sin internet, incluida la Semana Santa) y santoral del día. Se elige en Ajustes → Calendario, con las 17 comunidades autónomas, Ceuta y Melilla (fechas fijas, Jueves Santo, Lunes de Pascua y Corpus según cada una).
- Resumen (dashboard) como primera pestaña: tarjetas de Notas, Tareas, Tablero y Calendario con sus números, gráfico de tareas de los próximos 7 días, tareas de hoy y atrasadas, avance de cada tablero y notas recientes. Cada tarjeta lleva a su sección.
- Varios tableros, uno por proyecto: chips de tableros en la pestaña Tablero, «+ Nuevo tablero», renombrar y eliminar desde el menú ⋮ (las tarjetas de un tablero eliminado pasan al primero). Copia de seguridad con campos opcionales `boards` y `boardId`; la base de datos pasa a la versión 8.
- Widgets de lista de tareas, agenda y notas fijadas (3×3, redimensionables). Tocar una fila abre esa nota y se actualizan solos al cambiar las notas.
- Bloqueo con huella: en Ajustes → Seguridad. Pide huella, cara o el bloqueo de pantalla del móvil al abrir la app o tras 15 s en segundo plano, y oculta la app en capturas y apps recientes.
- Exportar a Markdown (Ajustes): un ZIP con un archivo `.md` por nota (carpetas por tipo, con cabecera YAML con fecha, etiquetas y prioridad).
- Comandos con `/` en el editor: escribe `/` al final del texto para insertar tarea, título, lista, cita, separador, fecha, hora, enlace, mención o subpágina.
- Enlaces entre notas con `[[Título]]`, menciones con `@` y «Mencionada en» (retroenlaces).
- Subpáginas: una nota puede contener otras (campo opcional `parentId` en la copia de seguridad). Solo se ven dentro de su nota o al buscar.
- Papelera: borrar envía a la papelera (30 días, luego se vacía sola) desde donde se puede restaurar o eliminar para siempre (campo opcional `deletedAt`). La base de datos pasa a la versión 7.
- Propiedades personalizadas: prioridad (baja, media, alta) en las tareas y etiquetas en cualquier nota. Se eligen en el editor, se ven como chips en Inicio, Tareas y el tablero, y tocar una etiqueta en Inicio filtra por ella. Entran en la copia de seguridad (campos opcionales `priority` y `tags`) y la base de datos pasa a la versión 6.
- Vistas guardadas: en Inicio, elige un filtro o escribe una búsqueda y pulsa «+ Guardar vista» para darle nombre; aparece como chip. Un toque la aplica y una pulsación larga la elimina.
- Tareas que se repiten (cada día, semana, mes o año): al marcarlas como hechas pasan solas a su siguiente fecha, pendientes otra vez y con su aviso reprogramado. Se elige en el editor de la tarea, se guarda en la copia de seguridad (campo opcional `repeat`) y la base de datos pasa a la versión 5.
- Calendario: vista de agenda (interruptor Mes/Agenda) con las tareas vencidas y las próximas agrupadas por día.
- Tablero: arrastrar tarjetas con pulsación larga entre columnas y dentro de una columna (el tablero se desplaza al acercar el dedo al borde). El menú ⋮ sigue disponible.
- Apariencia: icono en Inicio para cambiar entre tema claro y oscuro, y en Ajustes elegir Sistema/Claro/Oscuro y activar los colores dinámicos (Material You, Android 12+).
- Ajustes → «Cargar datos de ejemplo» (y «Quitar»): rellena notas, compra, tareas, tablero y bandeja para ver el diseño. Inicio con chips y botón verdes, botones redondos, barra de progreso y fecha en las tarjetas.
- Plantillas: compra semanal, reunión, proyecto y diario, desde el icono de lista de la pantalla de notas.
- Modo compra: filas grandes, pantalla siempre encendida, los artículos marcados bajan al final, con progreso y botones «Desmarcar todo» y «Quitar marcados». Se abre desde el carrito del editor de una lista de compra.
- Calendario mensual: puntos en los días con tareas, lista de tareas del día elegido y botón para crear una con esa fecha.
- Recordatorios con notificación: elige una hora en una tarea con fecha; botón «Marcar como hecha» en el aviso. Se reprograman solo al reiniciar el móvil (Android borra las alarmas al apagarse). Permisos: notificaciones y alarmas exactas (opcional).
- Tareas con fecha límite: pestaña Tareas agrupada en vencidas, hoy, próximos 7 días, más adelante, sin fecha y hechas; marcar como hecha con un toque y elegir la fecha en el editor. La fecha también se ve en las tarjetas del tablero.
- Tablero estilo Trello: columnas editables (renombrar, mover, borrar) y tarjetas que son tus tareas; mover entre columnas y reordenar desde el menú de cada tarjeta. Se incluye en la copia de seguridad.
- Idiomas: castellano e inglés (selector por app en Android 13+, idioma del sistema en versiones anteriores).
- Licencia GPL-3.0-or-later y Gradle wrapper (`gradlew`).
- Primer esqueleto del código (Kotlin, Compose, Room): notas, ideas, tareas y compra con listas, bandeja de entrada, captura rápida desde widget y desde «Compartir».
- Exportar e importar todo el contenido (ZIP con `backup.json`) para cambiar de dispositivo; ver `docs/BACKUP_FORMAT.md`.
- Script `scripts/rename-package.sh` para fijar tu propio identificador de app.
- Análisis de apps existentes (F-Droid y comerciales) y definición de funciones por versión.
- Mockups de 12 pantallas: inicio, tablero, calendario, editor de lista, widgets, bandeja de entrada, vistas guardadas, propiedades y enlaces, plantillas, captura rápida y atajos, modo compra y comandos con `/`.
- Documentación inicial: README, guía de contribución, hoja de ruta y guía de publicación en F-Droid.
- Textos de la ficha de F-Droid (español e inglés).
