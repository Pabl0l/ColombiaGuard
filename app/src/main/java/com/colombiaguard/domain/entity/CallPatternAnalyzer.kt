package com.colombiaguard.domain.entity

/**
 * Analizador de patrones de spam telefónico.
 * Detecta comportamientos sospechosos basados en:
 * - Frecuencia de llamadas
 * - Duración de las llamadas
 * - Patrones de numeración
 * - Comportamiento de colgado rápido
 */
object CallPatternAnalyzer {

    /**
     * Ventana de tiempo para análisis de patrones (en milisegundos).
     * 24 horas para detectar patrones repetidos.
     */
    private const val PATTERN_WINDOW_MS = 24 * 60 * 60 * 1000L

    /**
     * Tiempo mínimo para considerar una llamada "conectada" (5 segundos).
     * Las llamadas spam típicamente duran menos de 3 segundos.
     */
    private const val MIN_CONNECTED_CALL_DURATION_MS = 5000L

    /**
     * Número máximo de llamadas desde un número en la ventana de tiempo
     * antes de considerarlo spam.
     */
    private const val MAX_CALLS_PER_NUMBER_IN_WINDOW = 3

    /**
     * Tiempo mínimo entre llamadas del mismo número para considerarlo spam (5 minutos).
     */
    private const val MIN_TIME_BETWEEN_CALLS_MS = 5 * 60 * 1000L

    /** Longitud máxima del bloque que se comprueba como patrón repetido. */
    private const val MAX_REPEAT_BLOCK = 5

    data class SpamIndicator(
        val type: SpamType,
        val confidence: Float, // 0.0 a 1.0
        val description: String
    )

    enum class SpamType {
        /** Llamada que cuelga inmediatamente (< 5 segundos) */
        QUICK_HANGUP,
        /** Múltiples llamadas del mismo número en corto tiempo */
        REPEATED_CALLS,
        /** Número que nunca ha sido contactado antes */
        UNKNOWN_NUMBER,
        /** Patrón de numeración sospechoso (números secuenciales, etc.) */
        SUSPICIOUS_PATTERN,
        /** Verificación de número falló */
        FAILED_VERIFICATION,
        /** Llamada fuera de horario comercial */
        OFF_HOURS_CALL
    }

    /**
     * Analiza una llamada y retorna indicadores de spam.
     * No toma decisiones, solo proporciona evidencia.
     */
    fun analyzeCall(
        callInfo: CallInfo,
        recentCalls: List<CallInfo> = emptyList()
    ): List<SpamIndicator> {
        val indicators = mutableListOf<SpamIndicator>()

        // 1. Analizar colgado rápido
        if (callInfo.duration in 1 until MIN_CONNECTED_CALL_DURATION_MS) {
            indicators.add(
                SpamIndicator(
                    type = SpamType.QUICK_HANGUP,
                    confidence = 0.8f,
                    description = "Llamada duró ${callInfo.duration / 1000}s (menos de ${MIN_CONNECTED_CALL_DURATION_MS / 1000}s)"
                )
            )
        }

        // 2. Analizar llamadas repetidas del mismo número
        val sameNumberCalls = recentCalls.filter {
            PhoneNumbers.sameNumber(it.phoneNumber, callInfo.phoneNumber) &&
            (callInfo.timestamp - it.timestamp) in 0..PATTERN_WINDOW_MS
        }

        if (sameNumberCalls.size >= MAX_CALLS_PER_NUMBER_IN_WINDOW) {
            indicators.add(
                SpamIndicator(
                    type = SpamType.REPEATED_CALLS,
                    confidence = 0.9f,
                    description = "${sameNumberCalls.size} llamadas en 24h desde este número"
                )
            )
        }

        // 3. Analizar intervalo entre llamadas
        val lastCallFromNumber = recentCalls
            .filter { PhoneNumbers.sameNumber(it.phoneNumber, callInfo.phoneNumber) }
            .maxByOrNull { it.timestamp }

        if (lastCallFromNumber != null) {
            val interval = callInfo.timestamp - lastCallFromNumber.timestamp
            if (interval in 0 until MIN_TIME_BETWEEN_CALLS_MS) {
                indicators.add(
                    SpamIndicator(
                        type = SpamType.REPEATED_CALLS,
                        confidence = 0.85f,
                        description = "Llamada anterior hace ${interval / 1000}s (menos de ${MIN_TIME_BETWEEN_CALLS_MS / 1000}s)"
                    )
                )
            }
        }

        // 4. Verificación falló
        if (callInfo.verificationStatus == VerificationStatus.FAILED) {
            indicators.add(
                SpamIndicator(
                    type = SpamType.FAILED_VERIFICATION,
                    confidence = 0.95f,
                    description = "La red verificó que el número NO es válido (posible spoofing)"
                )
            )
        }

        // 5. Patrón de numeración sospechoso
        if (hasSuspiciousPattern(callInfo.phoneNumber)) {
            indicators.add(
                SpamIndicator(
                    type = SpamType.SUSPICIOUS_PATTERN,
                    confidence = 0.7f,
                    description = "Patrón de numeración sospechoso detectado"
                )
            )
        }

        return indicators
    }

    /**
     * Calcula el nivel de riesgo basado en los indicadores.
     * Retorna un score de 0.0 (sin riesgo) a 1.0 (alto riesgo).
     */
    fun calculateRiskScore(indicators: List<SpamIndicator>): Float {
        if (indicators.isEmpty()) return 0f

        // Promedio ponderado por confianza
        val totalConfidence = indicators.sumOf { it.confidence.toDouble() }
        val maxPossibleConfidence = indicators.size.toDouble()

        return (totalConfidence / maxPossibleConfidence).toFloat()
    }

    /**
     * Detecta patrones numéricos sospechosos.
     * - Todos los dígitos iguales (1111111)
     * - Secuencia ascendente/descendente completa (1234567, 7654321)
     * - Bloque repetido en todo el número (123123123, 3003003000)
     *
     * No se usa "contiene 1234567": un móvil real como 3001234567 contiene esa
     * secuencia y quedaría marcado como spam (falso positivo inaceptable).
     */
    private fun hasSuspiciousPattern(phoneNumber: String): Boolean {
        val digits = PhoneNumbers.normalize(phoneNumber)

        if (digits.length < 5) return false

        // Todos los dígitos iguales (111111, 222222, etc.)
        if (digits.all { it == digits[0] }) return true

        // Secuencia ascendente (123456) o descendente (654321)
        if (isStepSequence(digits, 1) || isStepSequence(digits, -1)) return true

        return hasRepeatingBlock(digits)
    }

    private fun isStepSequence(digits: String, step: Int): Boolean =
        digits.zipWithNext().all { (a, b) -> b.digitToInt() - a.digitToInt() == step }

    /** Bloque que se repite de principio a fin: 123123123, 0101010101. */
    private fun hasRepeatingBlock(digits: String): Boolean =
        (1..MAX_REPEAT_BLOCK).any { period ->
            digits.length % period == 0 &&
                digits.take(period).repeat(digits.length / period) == digits
        }

    /**
     * Verifica si una llamada es fuera de horario comercial.
     * Horario comercial: 7:00 AM - 9:00 PM (Colombia)
     */
    fun isOffHours(timestamp: Long): Boolean {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Bogota"))
        calendar.timeInMillis = timestamp
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        return hour < 7 || hour >= 21
    }
}
