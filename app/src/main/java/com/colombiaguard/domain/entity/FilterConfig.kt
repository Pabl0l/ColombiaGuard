package com.colombiaguard.domain.entity

/**
 * Niveles de filtrado disponibles.
 * El usuario puede ajustar la aggressividad del filtro.
 */
enum class FilterLevel {
    /**
     * Estricto: Solo permitir números colombianos conocidos.
     * Bloquea todo lo que no sea +57 válido.
     */
    STRICT,

    /**
     * Normal: Permitir colombianos + silenciar desconocidos.
     * Los extranjeros se silencian, no se bloquean (pueden ser contactos).
     */
    NORMAL,

    /**
     * Permisivo: Solo bloquear spam confirmado y números extranjeros cortos.
     * Permite casi todo, solo bloquea patrones obvios de spam.
     */
    PERMISSIVE
}

/**
 * Configuración del filtro almacenada en preferences.
 */
data class FilterConfig(
    val level: FilterLevel = FilterLevel.NORMAL,
    val blockForeignCalls: Boolean = true,
    val silenceUnknownColombian: Boolean = false,
    val enablePatternDetection: Boolean = true,
    val whitelistAllContacts: Boolean = true
)
