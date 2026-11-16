package io.github.estebangocoalt.blocdenotas.data

/**
 * Datos de ejemplo para ver el diseño con contenido. Todos los ids empiezan por [PREFIX],
 * así se pueden quitar sin tocar nada de lo que haya escrito el usuario.
 * Lógica pura (sin Android).
 */
object DemoData {
    const val PREFIX = "demo-"

    class Result(
        val notes: List<NoteEntity>,
        val items: List<ChecklistItemEntity>,
    )

    /**
     * [columnIds] son las columnas del tablero (en orden) donde repartir las tarjetas;
     * [todayEpochDay] es el día local de hoy.
     */
    fun build(es: Boolean, columnIds: List<String>, todayEpochDay: Long, now: Long): Result {
        val notes = mutableListOf<NoteEntity>()
        val items = mutableListOf<ChecklistItemEntity>()
        var order = 0

        fun t(esText: String, enText: String) = if (es) esText else enText
        fun due(offset: Int) = TaskLogic.dueAtOfDay(todayEpochDay + offset)

        fun add(
            key: String,
            kind: NoteKind,
            title: String,
            body: String = "",
            color: Int = kind.defaultColor,
            pinned: Boolean = false,
            dueOffset: Int? = null,
            minute: Int? = null,
            done: Boolean = false,
            column: Int? = null,
            inbox: Boolean = false,
            checklist: List<Pair<String, Boolean>> = emptyList(),
        ) {
            val id = PREFIX + key
            val stamp = now - (order++) * 60_000L
            notes += NoteEntity(
                id = id,
                title = title,
                body = body,
                kind = kind,
                colorIndex = color,
                pinned = pinned,
                inbox = inbox,
                dueAt = dueOffset?.let(::due),
                createdAt = stamp,
                updatedAt = stamp,
                completedAt = if (done) stamp else null,
                reminderMinute = minute,
                columnId = column?.let { columnIds.getOrNull(it.coerceAtMost(columnIds.lastIndex)) },
                columnPosition = order,
            )
            checklist.forEachIndexed { i, (text, checked) ->
                items += ChecklistItemEntity(
                    id = "$id-i$i", noteId = id, text = text, checked = checked, position = i,
                )
            }
        }

        // Notas e ideas
        add(
            "note-meeting", NoteKind.NOTE, t("Reunión con el equipo", "Team meeting"),
            t(
                "Revisar el calendario de entregas y repartir las tareas de la semana. Pedir feedback del último sprint.",
                "Review the delivery calendar and split this week's tasks. Ask for feedback on the last sprint.",
            ),
            color = 0, pinned = true,
        )
        add(
            "idea-app", NoteKind.IDEA, t("Idea: app de rutas", "Idea: routes app"),
            t(
                "Mapa con los puntos de recogida del día y botón de «hecho» por parada. Funcionaría sin conexión.",
                "Map with the day's pickup points and a \"done\" button per stop. It would work offline.",
            ),
            color = 3,
        )
        add(
            "note-recipe", NoteKind.NOTE, t("Receta de la abuela", "Grandma's recipe"),
            t(
                "Sofrito lento con cebolla y tomate rallado. Añadir el pimentón al final para que no amargue.",
                "Slow soffritto with onion and grated tomato. Add the paprika at the end so it doesn't turn bitter.",
            ),
            color = 2,
        )
        add(
            "idea-gift", NoteKind.IDEA, t("Regalo de cumpleaños", "Birthday gift"),
            t("Un álbum con fotos del año y una nota escrita a mano.", "An album with photos from the year and a handwritten note."),
            color = 4,
        )

        // Compra
        add(
            "shop-weekly", NoteKind.SHOPPING, t("Compra semanal", "Weekly shopping"),
            color = 1, pinned = true,
            checklist = listOf(
                t("Leche", "Milk") to true,
                t("Huevos", "Eggs") to false,
                t("Pan de molde", "Sandwich bread") to false,
                t("Tomates", "Tomatoes") to false,
                t("Pasta", "Pasta") to true,
                t("Yogures", "Yogurts") to false,
            ),
        )
        add(
            "shop-hardware", NoteKind.SHOPPING, t("Ferretería", "Hardware store"),
            color = 5,
            checklist = listOf(
                t("Tornillos 4 mm", "4 mm screws") to false,
                t("Cinta aislante", "Electrical tape") to false,
                t("Bombilla LED", "LED bulb") to true,
            ),
        )

        // Tareas con fecha
        add(
            "task-overdue", NoteKind.TASK, t("Pagar el recibo de la luz", "Pay the electricity bill"),
            dueOffset = -2, color = 2,
        )
        add(
            "task-today", NoteKind.TASK, t("Llamar al dentista", "Call the dentist"),
            dueOffset = 0, minute = 10 * 60, color = 4,
        )
        add(
            "task-today2", NoteKind.TASK, t("Entregar el producto del módulo", "Submit the module project"),
            dueOffset = 0, minute = 18 * 60, color = 0, column = 1,
            checklist = listOf(
                t("Memoria en PDF", "PDF report") to true,
                t("Repositorio limpio", "Clean repository") to true,
                t("Capturas", "Screenshots") to false,
            ),
        )
        add("task-soon", NoteKind.TASK, t("Reunión del colegio", "School meeting"), dueOffset = 3, minute = 17 * 60 + 30, color = 1)
        add("task-week", NoteKind.TASK, t("Renovar la ITV", "Renew the car inspection"), dueOffset = 6, color = 3)
        add("task-later", NoteKind.TASK, t("Reservar vacaciones", "Book holidays"), dueOffset = 20, color = 0)
        add("task-done", NoteKind.TASK, t("Enviar el currículum", "Send the CV"), dueOffset = -1, done = true, color = 1)

        // Tarjetas del tablero
        add(
            "card-1", NoteKind.TASK, t("Diseñar la pantalla de inicio", "Design the home screen"),
            column = 0, color = 4,
        )
        add("card-2", NoteKind.TASK, t("Escribir la documentación", "Write the documentation"), column = 0, color = 4)
        add(
            "card-3", NoteKind.TASK, t("Probar las notificaciones", "Test the notifications"),
            column = 1, dueOffset = 2, color = 4,
            checklist = listOf(
                t("Permiso de avisos", "Notification permission") to true,
                t("Alarma exacta", "Exact alarm") to false,
            ),
        )
        add("card-4", NoteKind.TASK, t("Publicar en F-Droid", "Publish on F-Droid"), column = 1, color = 4)
        add("card-5", NoteKind.TASK, t("Crear el repositorio", "Create the repository"), column = 2, done = true, color = 4)
        add("card-6", NoteKind.TASK, t("Elegir la licencia", "Choose the license"), column = 2, done = true, color = 4)

        // Bandeja
        add("inbox-1", NoteKind.NOTE, "", t("comprar pilas y detergente", "buy batteries and detergent"), inbox = true)
        add("inbox-2", NoteKind.NOTE, "", t("llamar a mamá el domingo", "call mom on Sunday"), inbox = true)
        add("inbox-3", NoteKind.NOTE, "", t("idea: widget con la lista de la compra", "idea: shopping list widget"), inbox = true)

        return Result(notes, items)
    }
}
