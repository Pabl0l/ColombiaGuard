package com.colombiaguard.domain.entity

/**
 * Clasificación resultante del análisis de una llamada.
 */
enum class CallClassification {
	/** Permitir - número conocido o colombiano válido */
	ALLOW,
	/** Silenciar - número desconocido, permitir pero sin sonar */
	SILENCE,
	/** Bloquear - spam detectado o número extranjero */
	BLOCK,
	/** Permitir siempre - está en la whitelist del usuario */
	WHITELISTED
}

/**
 * Resultado completo del análisis de filtrado.
 */
data class ScreeningResult(
	val classification: CallClassification,
	val reason: String,
	val isColombian: Boolean,
	val isKnownSpamPattern: Boolean = false,
	/** Etiqueta del número detectado ("Móvil", "Fijo · Bogotá…"). */
	val matchedLabel: String? = null
)
