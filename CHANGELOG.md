# Historial de cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado semántico.

## [Sin publicar]

### Añadido
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
