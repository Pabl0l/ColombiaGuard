package com.colombiaguard.domain.entity

/**
 * Plan de numeración colombiano.
 *
 * La detección es **estructural**, no una lista de prefijos móviles: en Colombia
 * existe portabilidad numérica desde 2011, así que asociar un prefijo a un
 * operador es información falsa, y una lista incompleta de prefijos haría que se
 * bloquearan móviles legítimos. Reglas aplicadas (CRC / MinTIC):
 *
 * - Móvil: 10 dígitos que empiezan por `3`.
 * - Fijo: 10 dígitos `60X` + 7 dígitos, con `X` en el plan de áreas vigente.
 * - Especial: códigos de emergencia y líneas de atención (ver [PhoneNumbers]).
 * - Extranjero: cualquier otra numeración con longitud internacional.
 *
 * Fuentes: CRC (marcación única nacional), MinTIC, operadores (Tigo, Claro, Movistar).
 */
object ColombianNumberRanges {

	/**
	 * Códigos de área fijos vigentes (marcación única nacional).
	 * El prefijo local dejó de existir en 2011: hoy se marca 60X + 7 dígitos.
	 */
	private val FIXED_AREA_CODES = mapOf(
		"601" to "Bogotá D.C. / Cundinamarca",
		"602" to "Cali / Cauca / Nariño / Valle",
		"604" to "Medellín / Antioquia / Córdoba / Chocó",
		"605" to "Barranquilla / Atlántico / Bolívar / Cesar / La Guajira / Magdalena / Sucre",
		"606" to "Pereira / Caldas / Quindío / Risaralda",
		"607" to "Bucaramanga / Arauca / Norte de Santander / Santander",
		"608" to "Neiva / Boyacá / Caquetá / Casanare / Huila / Meta / Tolima / Amazonía"
	)

	/** Indicativos de país usados solo para etiquetar llamadas extranjeras. */
	private val FOREIGN_COUNTRIES = mapOf(
		"1" to "USA/Canadá",
		"7" to "Rusia",
		"20" to "Egipto",
		"27" to "Sudáfrica",
		"33" to "Francia",
		"34" to "España",
		"39" to "Italia",
		"44" to "Reino Unido",
		"49" to "Alemania",
		"51" to "Perú",
		"52" to "México",
		"54" to "Argentina",
		"55" to "Brasil",
		"56" to "Chile",
		"57" to "Colombia",
		"61" to "Australia",
		"81" to "Japón",
		"82" to "Corea del Sur",
		"86" to "China",
		"91" to "India",
		"234" to "Nigeria"
	)

	data class NumberInfo(
		val isValid: Boolean,
		val isColombian: Boolean,
		val isMobile: Boolean,
		val isFixed: Boolean,
		val isSpecial: Boolean,
		val isForeign: Boolean,
		/** Descripción apta para mostrar al usuario ("Móvil", "Fijo · Bogotá…"). */
		val label: String,
		val category: String,
		/** Dígitos canónicos (ver [PhoneNumbers.normalize]). */
		val normalizedNumber: String
	)

	/**
	 * Analiza un número y devuelve su clasificación. Nunca lanza: una entrada
	 * basura se clasifica como "Desconocido".
	 */
	fun analyzeNumber(phoneNumber: String): NumberInfo {
		val digits = PhoneNumbers.normalize(phoneNumber)

		if (PhoneNumbers.isEmergency(digits)) {
			return NumberInfo(
				isValid = true,
				isColombian = true,
				isMobile = false,
				isFixed = false,
				isSpecial = true,
				isForeign = false,
				label = "Emergencias",
				category = "Especial",
				normalizedNumber = digits
			)
		}

		// Móvil: 10 dígitos que empiezan por 3.
		if (digits.length == 10 && digits.startsWith("3")) {
			return NumberInfo(
				isValid = true,
				isColombian = true,
				isMobile = true,
				isFixed = false,
				isSpecial = false,
				isForeign = false,
				label = "Móvil",
				category = "Móvil",
				normalizedNumber = digits
			)
		}

		// Fijo: 60X + 7 dígitos, con área vigente.
		if (digits.length == 10 && digits.startsWith("60")) {
			val area = FIXED_AREA_CODES[digits.take(3)]
			if (area != null) {
				return NumberInfo(
					isValid = true,
					isColombian = true,
					isMobile = false,
					isFixed = true,
					isSpecial = false,
					isForeign = false,
					label = "Fijo · $area",
					category = "Fijo",
					normalizedNumber = digits
				)
			}
		}

		// Cualquier otra numeración larga se considera extranjera.
		if (digits.length >= 11) {
			val country = detectCountry(digits)
			return NumberInfo(
				isValid = PhoneNumbers.hasValidFormat(digits),
				isColombian = false,
				isMobile = false,
				isFixed = false,
				isSpecial = false,
				isForeign = true,
				label = country?.let { "Extranjero · $it" } ?: "Extranjero",
				category = "Extranjero",
				normalizedNumber = digits
			)
		}

		// Ni colombiano ni extranjero reconocible (cortos, códigos de servicio…).
		return NumberInfo(
			isValid = PhoneNumbers.hasValidFormat(digits),
			isColombian = false,
			isMobile = false,
			isFixed = false,
			isSpecial = false,
			isForeign = false,
			label = "Desconocido",
			category = "Desconocido",
			normalizedNumber = digits
		)
	}

	/** Verifica si un número tiene un formato plausible (E.164: 7 a 15 dígitos). */
	fun hasValidFormat(phoneNumber: String): Boolean = PhoneNumbers.hasValidFormat(phoneNumber)

	private fun detectCountry(digits: String): String? {
		// Prefijos de 1 a 3 dígitos, del más largo al más corto.
		return (3 downTo 1)
			.mapNotNull { length -> digits.take(length).takeIf { it.length == length } }
			.firstNotNullOfOrNull { prefix -> FOREIGN_COUNTRIES[prefix] }
	}
}
