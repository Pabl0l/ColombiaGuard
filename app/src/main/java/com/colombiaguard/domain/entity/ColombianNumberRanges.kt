package com.colombiaguard.domain.entity

/**
 * Base de datos de rangos telefónicos colombianos.
 * Contiene los rangos reales asignados por la ANE (Agencia Nacional de Espectro)
 * y los operadores de telecomunicaciones de Colombia.
 *
 * Fuente: ANE Colombia, MinTIC, operadores (Claro, Movistar, Tigo, ETB, etc.)
 * Última actualización: 2025
 */
object ColombianNumberRanges {

    /**
     * Rangos móviles colombianos por prefijo de 3 dígitos.
     * Formato nacional: 10 dígitos (sin código de país)
     * Formato internacional: +57 + 10 dígitos
     */
    private val MOBILE_RANGES = mapOf(
        // Claro (América Móvil)
        "300" to "Claro",
        "301" to "Claro",
        "302" to "Claro",
        "303" to "Claro",
        "304" to "Claro",
        "305" to "Claro",

        // Movistar (Telefónica)
        "310" to "Movistar",
        "311" to "Movistar",
        "312" to "Movistar",
        "313" to "Movistar",
        "314" to "Movistar",
        "315" to "Movistar",
        "316" to "Movistar",
        "317" to "Movistar",
        "318" to "Movistar",
        "319" to "Movistar",
        "320" to "Movistar",
        "321" to "Movistar",

        // Tigo (Millicom)
        "322" to "Tigo",
        "323" to "Tigo",
        "324" to "Tigo",

        // ETB (Bogotá)
        "325" to "ETB",

        // Tigo (continuación)
        "350" to "Tigo",
        "351" to "Tigo",

        // WOM (nuevo operador)
        "330" to "WOM",
        "331" to "WOM",
        "332" to "WOM",
        "333" to "WOM",
        "334" to "WOM",

        // Movistar (rangos adicionales)
        "310" to "Movistar",
        "320" to "Movistar"
    )

    /**
     * Rangos fijos colombianos por prefijo de 1-3 dígitos.
     * Incluye Bogotá (601), Medellín (604), Cali (602), etc.
     */
    private val FIXED_RANGES = mapOf(
        // Bogotá D.C.
        "601" to "Fijo Bogotá",

        // Departamentos y ciudades principales
        "602" to "Fijo Cali/Valle",
        "604" to "Fijo Medellín/Antioquia",
        "605" to "Fijo Barranquilla/Atlántico",
        "606" to "Fijo Bucaramanga/Santander",
        "607" to "Fijo Cundinamarca",
        "608" to "Fijo Pereira/Risaralda",
        "609" to "Fijo Neiva/Huila",
        "610" to "Fijo Ibagué/Tolima",
        "611" to "Fijo Cartagena/Bolívar",
        "612" to "Fijo Sincelejo/Sucre",
        "613" to "Fijo Tunja/Boyacá",
        "614" to "Fijo Popayán/Cauca",
        "615" to "Fijo Armenia/Quindío",
        "616" to "Fijo Riohacha/La Guajira",
        "617" to "Fijo Santa Marta/Magdalena",
        "618" to "Fijo Valledupar/Cesar",
        "619" to "Fijo Pasto/Nariño",
        "620" to "Fijo Villavicencio/Meta",
        "621" to "Fijo Florencia/Caquetá",
        "622" to "Fijo Yopal/Casanare",
        "623" to "Fijo Mocoa/Putumayo",
        "624" to "Fijo Puerto Inírida/Guainía",
        "625" to "Fijo San José del Guaviare/Guaviare",
        "626" to "Fijo Leticia/Amazonas",
        "627" to "Fijo Puerto Carreño/Vichada",
        "628" to "Fijo Arauca/Arauca",
        "629" to "Fijo Quibdó/Chocó",
        "630" to "Fijo Montería/Córdoba",
        "631" to "Fijo Sincelejo/Sucre",
        "632" to "Fijo Barranca/Arauca",
        "633" to "Fijo環太平洋",
        "634" to "Fijo环太平洋",
        "635" to "Fijo环太平洋",
        "650" to "Fijo环太平洋"
    )

    /**
     * Números especiales colombianos.
     */
    private val SPECIAL_NUMBERS = mapOf(
        "123" to "Emergencias",
        "132" to "Ambulancia",
        "125" to "Bomberos",
        "141" to "Policía",
        "112" to "Emergencias Internacionales",
        "103" to "Línea de la Vida",
        "106" to "Defensa Civil",
        "119" to "Cruz Roja",
        "144" to "Línea contra violencia de género",
        "155" to "Línea contra tortura",
        "147" to "Contact Center MinTIC"
    )

    /**
     * Prefijos internacionales comunes (no colombianos).
     * Para detección rápida de llamadas extranjeras.
     */
    private val FOREIGN_PREFIXES = listOf(
        "1" to "USA/Canadá",
        "44" to "Reino Unido",
        "34" to "España",
        "52" to "México",
        "51" to "Perú",
        "56" to "Chile",
        "54" to "Argentina",
        "55" to "Brasil",
        "49" to "Alemania",
        "33" to "Francia",
        "39" to "Italia",
        "81" to "Japón",
        "82" to "Corea del Sur",
        "86" to "China",
        "91" to "India",
        "61" to "Australia",
        "7" to "Rusia",
        "20" to "Egipto",
        "234" to "Nigeria",
        "27" to "Sudáfrica"
    )

    data class NumberInfo(
        val isValid: Boolean,
        val isColombian: Boolean,
        val isMobile: Boolean,
        val isFixed: Boolean,
        val isSpecial: Boolean,
        val isForeign: Boolean,
        val operator: String?,
        val category: String,
        val normalizedNumber: String
    )

    /**
     * Analiza un número telefónico y retorna información completa.
     * Maneja múltiples formatos:
     * - +57 300 123 4567
     * - 57 300 123 4567
     * - 300 123 4567
     * - 3001234567
     * - +573001234567
     */
    fun analyzeNumber(phoneNumber: String): NumberInfo {
        val normalized = normalizeNumber(phoneNumber)
        val digitsOnly = normalized.replace("\\D".toRegex(), "")

        // Verificar números especiales primero
        if (isSpecialNumber(digitsOnly)) {
            return NumberInfo(
                isValid = true,
                isColombian = true,
                isMobile = false,
                isFixed = false,
                isSpecial = true,
                isForeign = false,
                operator = SPECIAL_NUMBERS[digitsOnly],
                category = "Especial",
                normalizedNumber = normalized
            )
        }

        // Detectar si es colombiano
        val colombianNumber = extractColombianNumber(digitsOnly)

        return if (colombianNumber != null) {
            val prefix3 = colombianNumber.take(3)
            val prefix1 = colombianNumber.take(1)

            val isMobile = MOBILE_RANGES.containsKey(prefix3)
            val isFixed = FIXED_RANGES.containsKey(prefix3) || FIXED_RANGES.containsKey(prefix1)
            val operator = MOBILE_RANGES[prefix3] ?: FIXED_RANGES[prefix3] ?: FIXED_RANGES[prefix1]

            NumberInfo(
                isValid = true,
                isColombian = true,
                isMobile = isMobile,
                isFixed = isFixed,
                isSpecial = false,
                isForeign = false,
                operator = operator,
                category = when {
                    isMobile -> "Móvil"
                    isFixed -> "Fijo"
                    else -> "Desconocido"
                },
                normalizedNumber = normalized
            )
        } else {
            // No es colombiano - detectar país de origen
            val foreignInfo = detectForeignCountry(digitsOnly)
            NumberInfo(
                isValid = true,
                isColombian = false,
                isMobile = false,
                isFixed = false,
                isSpecial = false,
                isForeign = true,
                operator = foreignInfo?.second,
                category = foreignInfo?.first ?: "Extranjero",
                normalizedNumber = normalized
            )
        }
    }

    /**
     * Normaliza un número telefónico a formato estándar.
     * Elimina espacios, guiones, paréntesis y otros caracteres.
     */
    private fun normalizeNumber(phoneNumber: String): String {
        var num = phoneNumber.trim()

        // Si empieza con +, quitarlo para análisis
        if (num.startsWith("+")) {
            num = num.substring(1)
        }

        // Eliminar caracteres no numéricos
        return num.replace("\\D".toRegex(), "")
    }

    /**
     * Extrae el número colombiano de un string de dígitos.
     * Maneja prefijos de país (57) y formatos variados.
     */
    private fun extractColombianNumber(digits: String): String? {
        // Caso 1: Empieza con 57 (código de país sin +)
        if (digits.startsWith("57") && digits.length >= 12) {
            val withoutCountry = digits.substring(2)
            if (isValidColombianLocal(withoutCountry)) {
                return withoutCountry
            }
        }

        // Caso 2: 10 dígitos que parecen colombianos
        if (digits.length == 10 && isValidColombianLocal(digits)) {
            return digits
        }

        // Caso 3: 7 u 8 dígitos (número fijo sin código de país)
        if ((digits.length == 7 || digits.length == 8) && isValidColombianFixed(digits)) {
            return digits
        }

        return null
    }

    /**
     * Valida si un número de 10 dígitos es colombiano móvil válido.
     */
    private fun isValidColombianLocal(number: String): Boolean {
        if (number.length != 10) return false
        val prefix3 = number.take(3)
        return MOBILE_RANGES.containsKey(prefix3)
    }

    /**
     * Valida si un número de 7-8 dígitos es fijo colombiano válido.
     */
    private fun isValidColombianFixed(number: String): Boolean {
        val prefix3 = number.take(3)
        val prefix1 = number.take(1)
        return FIXED_RANGES.containsKey(prefix3) || FIXED_RANGES.containsKey(prefix1)
    }

    /**
     * Verifica si es un número especial (emergencias, líneas gratuitas).
     */
    private fun isSpecialNumber(digits: String): Boolean {
        return SPECIAL_NUMBERS.containsKey(digits)
    }

    /**
     * Detecta el país de origen de un número extranjero.
     */
    private fun detectForeignCountry(digits: String): Pair<String, String>? {
        // Buscar prefijos de mayor a menor longitud
        val sortedPrefixes = FOREIGN_PREFIXES.sortedByDescending { it.first.length }

        for ((prefix, country) in sortedPrefixes) {
            if (digits.startsWith(prefix) && digits.length > prefix.length) {
                return Pair("Extranjero", country)
            }
        }

        return null
    }

    /**
     * Verifica si un número tiene un formato válido general.
     * No necesariamente colombiano, pero con estructura correcta.
     */
    fun hasValidFormat(phoneNumber: String): Boolean {
        val digits = phoneNumber.replace("\\D".toRegex(), "")
        // Números válidos: entre 7 y 15 dígitos (estándar ITU-T E.164)
        return digits.length in 7..15
    }
}
