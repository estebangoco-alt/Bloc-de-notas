package io.github.estebangocoalt.blocdenotas.backup

/** Error al leer una copia de seguridad. [reason] indica qué mensaje mostrar al usuario. */
class BackupException(
    val reason: Reason,
    cause: Throwable? = null,
) : Exception(reason.name, cause) {

    enum class Reason {
        /** El archivo no es una copia de esta app. */
        NOT_A_BACKUP,

        /** La copia la hizo una versión más nueva del formato. */
        UNSUPPORTED_VERSION,

        /** El archivo supera el límite de tamaño. */
        TOO_LARGE,

        /** El ZIP está dañado. */
        CORRUPT,
    }
}

enum class ImportMode {
    /** Añade lo nuevo y actualiza lo más antiguo. No borra nada. */
    MERGE,

    /** Borra lo que hay y deja solo el contenido de la copia. */
    REPLACE,
}
