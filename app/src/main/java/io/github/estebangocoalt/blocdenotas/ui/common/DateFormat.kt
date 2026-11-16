package io.github.estebangocoalt.blocdenotas.ui.common

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Muestra la fecha límite en el idioma actual (la fecha se guarda en UTC, ver TaskLogic). */
fun formatDueDate(dueAt: Long): String =
    Instant.ofEpochMilli(dueAt)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/** Hora del aviso (minutos desde las 00:00) en el formato del idioma actual, p. ej. «9:00» o «9:00 AM». */
fun formatMinute(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
