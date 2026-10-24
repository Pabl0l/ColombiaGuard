package com.colombiaguard.domain.entity

/**
 * Fuente única de verdad para normalización y validación de números.
 *
 * Regla del proyecto: **toda** comparación de números (whitelist, blacklist,
 * historial, contactos) se hace sobre el resultado de [normalize]. Guardar o
 * comparar el texto tal como lo escribe el usuario provoca fallos silenciosos
 * (el número "permitido" deja de coincidir con el que entrega la red).
 */
object PhoneNumbers {

	private val NON_DIGIT = Regex("\\D")
	private const val COUNTRY_CODE = "57"

	/**
	 * Códigos de emergencia y líneas de atención que nunca deben bloquearse.
	 * Solo se comparan por igualdad exacta: un número de 10 dígitos que
	 * *termine* en "123" NO es una emergencia.
	 */
	val EMERGENCY_CODES: Set<String> = setOf(
		"123", "112", "911", "000",
		"125", "132", "141", "144", "147", "155",
		"103", "106", "119", "110", "118"
	)

	/**
	 * Deja solo dígitos y elimina el código de país de Colombia cuando aplica.
	 *
	 * Ejemplos:
	 * - `"+57 300 123-4567"` → `"3001234567"`
	 * - `"0057 3001234567"` → `"3001234567"`
	 * - `"+1 305 123 4567"` → `"13051234567"`
	 */
	fun normalize(raw: String): String {
		var digits = raw.replace(NON_DIGIT, "")
		if (digits.startsWith("00")) digits = digits.substring(2)
		if (digits.length == 12 && digits.startsWith(COUNTRY_CODE)) digits = digits.substring(2)
		return digits
	}

	/** Compara dos números ignorando formato, espacios y prefijo de país. */
	fun sameNumber(a: String, b: String): Boolean {
		val left = normalize(a)
		return left.isNotEmpty() && left == normalize(b)
	}

	/** `true` si el número es un código de emergencia exacto. */
	fun isEmergency(raw: String): Boolean = normalize(raw) in EMERGENCY_CODES

	/** `true` si la estructura es plausible según el rango E.164 (7 a 15 dígitos). */
	fun hasValidFormat(raw: String): Boolean = normalize(raw).length in 7..15

	/** Representación legible para la UI. */
	fun formatDisplay(raw: String): String {
		val digits = normalize(raw)
		return when (digits.length) {
			10 -> "+57 ${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
			7 -> "+57 ${digits.substring(0, 3)} ${digits.substring(3)}"
			else -> raw.trim()
		}
	}
}
