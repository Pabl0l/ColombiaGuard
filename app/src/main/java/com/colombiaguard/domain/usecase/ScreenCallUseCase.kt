package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.*
import com.colombiaguard.domain.repository.ContactRepository
import com.colombiaguard.domain.repository.FilterConfigRepository
import com.colombiaguard.domain.repository.NumberListRepository

/**
 * Caso de uso principal: Filtrar una llamada entrante.
 *
 * Orquesta toda la lógica de filtrado:
 * 1. Verificar whitelist/blacklist
 * 2. Validar si es número colombiano
 * 3. Analizar patrones de spam
 * 4. Aplicar nivel de filtro configurado
 * 5. Retornar clasificación final
 *
 * Principios aplicados:
 * - SRP: Solo tiene una razón de cambio (reglas de filtrado)
 * - DIP: Depende de abstracciones (repositorios)
 * - OCP: Abierto a extensión (nuevas reglas) sin modificar existentes
 */
class ScreenCallUseCase(
    private val contactRepository: ContactRepository,
    private val filterConfigRepository: FilterConfigRepository,
    private val numberListRepository: NumberListRepository
) {

    /**
     * Ejecuta el filtrado completo de una llamada.
     *
     * @param callInfo Información de la llamada entrante
     * @param recentCalls Llamadas recientes para análisis de patrones
     * @return Resultado del screening con clasificación y razón
     */
    suspend fun execute(
        callInfo: CallInfo,
        recentCalls: List<CallInfo> = emptyList()
    ): ScreeningResult {

        // [REFUERZO 1] Guard: número vacío o muy corto → permitir
        if (callInfo.phoneNumber.isBlank() || callInfo.phoneNumber.replace("\\D".toRegex(), "").length < 3) {
            return ScreeningResult(
                classification = CallClassification.ALLOW,
                reason = "Número demasiado corto o vacío",
                isColombian = false
            )
        }

        // [REFUERZO 2] Verificar si es emergencia ESPECIAL PRIMERO (antes de blacklist)
        val isEmergency = isEmergencyNumber(callInfo.phoneNumber)
        if (isEmergency) {
            return ScreeningResult(
                classification = CallClassification.ALLOW,
                reason = "Número de emergencia",
                isColombian = true,
                matchedPrefix = callInfo.phoneNumber
            )
        }

        // 1. Verificar blacklist (emergencias ya pasaron arriba)
        if (numberListRepository.isBlacklisted(callInfo.phoneNumber)) {
            return ScreeningResult(
                classification = CallClassification.BLOCK,
                reason = "Número en lista negra",
                isColombian = false,
                isKnownSpamPattern = false
            )
        }

        // 2. Verificar whitelist (permiso inmediato)
        if (numberListRepository.isWhitelisted(callInfo.phoneNumber)) {
            return ScreeningResult(
                classification = CallClassification.WHITELISTED,
                reason = "Número en lista blanca",
                isColombian = false,
                isKnownSpamPattern = false
            )
        }

        // 3. Verificar contactos del usuario (con fallback seguro)
        val isContact = try {
            contactRepository.isNumberInContacts(callInfo.phoneNumber)
        } catch (e: Exception) {
            false // [REFUERZO 4] Si falla permiso de contactos, tratar como desconocido
        }

        // 4. Analizar número colombiano
        val numberInfo = ColombianNumberRanges.analyzeNumber(callInfo.phoneNumber)

        // 5. Obtener configuración actual
        val config = filterConfigRepository.getConfigOnce()

        // 6. Verificar si es número especial (emergencias siempre pasan)
        if (numberInfo.isSpecial) {
            return ScreeningResult(
                classification = CallClassification.ALLOW,
                reason = "Número de emergencia/especial",
                isColombian = true,
                matchedPrefix = callInfo.phoneNumber
            )
        }

        // [REFUERZO 7] Analizar horario nocturno como factor de riesgo
        val isNightTime = CallPatternAnalyzer.isOffHours(callInfo.timestamp)

        // 7. Aplicar reglas según nivel de filtro
        return when (config.level) {
            FilterLevel.STRICT -> applyStrictFilter(callInfo, numberInfo, config, recentCalls, isContact, isNightTime)
            FilterLevel.NORMAL -> applyNormalFilter(callInfo, numberInfo, config, recentCalls, isContact, isNightTime)
            FilterLevel.PERMISSIVE -> applyPermissiveFilter(callInfo, numberInfo, config, recentCalls, isContact, isNightTime)
        }
    }

    /**
     * Verifica si un número es de emergencia.
     * [REFUERZO 2] Siempre permite emergencias aunque estén en blacklist.
     */
    private fun isEmergencyNumber(phoneNumber: String): Boolean {
        val emergencyNumbers = listOf(
            "123", "132", "125", "141", "112",
            "103", "106", "119", "144", "155", "147",
            "911", "000", "110", "118", "119"
        )
        val normalized = phoneNumber.replace("\\D".toRegex(), "")
        return emergencyNumbers.any { normalized.endsWith(it) || normalized == it }
    }

    /**
     * Filtro estricto: Solo permitir números colombianos conocidos.
     */
    private suspend fun applyStrictFilter(
        callInfo: CallInfo,
        numberInfo: ColombianNumberRanges.NumberInfo,
        config: FilterConfig,
        recentCalls: List<CallInfo>,
        isContact: Boolean,
        isNightTime: Boolean
    ): ScreeningResult {
        // Números extranjeros siempre bloqueados en modo estricto
        if (numberInfo.isForeign) {
            return ScreeningResult(
                classification = CallClassification.BLOCK,
                reason = "Número extranjero bloqueado (modo estricto)",
                isColombian = false
            )
        }

        // Verificar verificación de red
        if (callInfo.verificationStatus == VerificationStatus.FAILED) {
            return ScreeningResult(
                classification = CallClassification.BLOCK,
                reason = "Verificación de número falló (posible spoofing)",
                isColombian = numberInfo.isColombian
            )
        }

        // Número colombiano válido
        if (numberInfo.isColombian) {
            // Analizar patrones de spam
            val indicators = CallPatternAnalyzer.analyzeCall(callInfo, recentCalls)
            var riskScore = CallPatternAnalyzer.calculateRiskScore(indicators)

            // [REFUERZO 3] Anti-spoofing: verificación falló + parece colombiano = alto riesgo
            if (callInfo.verificationStatus == VerificationStatus.FAILED && numberInfo.isColombian) {
                riskScore = (riskScore + 0.3f).coerceAtMost(1.0f)
            }

            // [REFUERZO 7] Noche + desconocido = riesgo adicional
            if (isNightTime && !isContact) {
                riskScore = (riskScore + 0.15f).coerceAtMost(1.0f)
            }

            return if (riskScore > 0.7f) {
                ScreeningResult(
                    classification = CallClassification.BLOCK,
                    reason = "Patrón de spam detectado (riesgo: ${(riskScore * 100).toInt()}%)",
                    isColombian = true,
                    isKnownSpamPattern = true,
                    matchedPrefix = numberInfo.operator
                )
            } else if (isContact || config.whitelistAllContacts) {
                ScreeningResult(
                    classification = CallClassification.ALLOW,
                    reason = "Número colombiano válido: ${numberInfo.operator ?: "Desconocido"}",
                    isColombian = true,
                    matchedPrefix = numberInfo.operator
                )
            } else {
                ScreeningResult(
                    classification = CallClassification.SILENCE,
                    reason = "Número colombiano desconocido",
                    isColombian = true,
                    matchedPrefix = numberInfo.operator
                )
            }
        }

        // Número no reconocido
        return ScreeningResult(
            classification = CallClassification.BLOCK,
            reason = "Número no reconocido como colombiano",
            isColombian = false
        )
    }

    /**
     * Filtro normal: Permitir colombianos + silenciar desconocidos.
     */
    private suspend fun applyNormalFilter(
        callInfo: CallInfo,
        numberInfo: ColombianNumberRanges.NumberInfo,
        config: FilterConfig,
        recentCalls: List<CallInfo>,
        isContact: Boolean,
        isNightTime: Boolean
    ): ScreeningResult {
        // Verificar verificación de red
        if (callInfo.verificationStatus == VerificationStatus.FAILED) {
            return ScreeningResult(
                classification = CallClassification.BLOCK,
                reason = "Verificación de número falló",
                isColombian = numberInfo.isColombian
            )
        }

        // Contactos siempre pasan
        if (isContact) {
            return ScreeningResult(
                classification = CallClassification.ALLOW,
                reason = "Contacto del usuario",
                isColombian = numberInfo.isColombian
            )
        }

        // Números extranjeros
        if (numberInfo.isForeign) {
            return if (config.blockForeignCalls) {
                ScreeningResult(
                    classification = CallClassification.BLOCK,
                    reason = "Llamada extranjera bloqueada",
                    isColombian = false
                )
            } else {
                ScreeningResult(
                    classification = CallClassification.SILENCE,
                    reason = "Llamada extranjera silenciada",
                    isColombian = false
                )
            }
        }

        // Números colombianos
        if (numberInfo.isColombian) {
            val indicators = CallPatternAnalyzer.analyzeCall(callInfo, recentCalls)
            var riskScore = CallPatternAnalyzer.calculateRiskScore(indicators)

            // [REFUERZO 3] Anti-spoofing
            if (callInfo.verificationStatus == VerificationStatus.FAILED && numberInfo.isColombian) {
                riskScore = (riskScore + 0.3f).coerceAtMost(1.0f)
            }

            // [REFUERZO 7] Noche + desconocido = riesgo adicional
            if (isNightTime && !isContact) {
                riskScore = (riskScore + 0.15f).coerceAtMost(1.0f)
            }

            return when {
                riskScore > 0.8f -> ScreeningResult(
                    classification = CallClassification.BLOCK,
                    reason = "Alto riesgo de spam (${(riskScore * 100).toInt()}%)",
                    isColombian = true,
                    isKnownSpamPattern = true
                )
                riskScore > 0.5f -> ScreeningResult(
                    classification = CallClassification.SILENCE,
                    reason = "Riesgo moderado de spam",
                    isColombian = true
                )
                config.silenceUnknownColombian -> ScreeningResult(
                    classification = CallClassification.SILENCE,
                    reason = "Número colombiano desconocido",
                    isColombian = true
                )
                else -> ScreeningResult(
                    classification = CallClassification.ALLOW,
                    reason = "Número colombiano válido: ${numberInfo.operator}",
                    isColombian = true
                )
            }
        }

        // Default: silenciar
        return ScreeningResult(
            classification = CallClassification.SILENCE,
            reason = "Número no reconocido",
            isColombian = false
        )
    }

    /**
     * Filtro permisivo: Solo bloquear spam confirmado.
     */
    private suspend fun applyPermissiveFilter(
        callInfo: CallInfo,
        numberInfo: ColombianNumberRanges.NumberInfo,
        config: FilterConfig,
        recentCalls: List<CallInfo>,
        isContact: Boolean,
        isNightTime: Boolean
    ): ScreeningResult {
        // Solo bloquear si hay alta confianza de spam
        val indicators = CallPatternAnalyzer.analyzeCall(callInfo, recentCalls)
        var riskScore = CallPatternAnalyzer.calculateRiskScore(indicators)

        // [REFUERZO 3] Anti-spoofing
        if (callInfo.verificationStatus == VerificationStatus.FAILED && numberInfo.isColombian) {
            riskScore = (riskScore + 0.3f).coerceAtMost(1.0f)
        }

        // [REFUERZO 7] Noche + desconocido = riesgo adicional
        if (isNightTime && !isContact) {
            riskScore = (riskScore + 0.15f).coerceAtMost(1.0f)
        }

        // Verificación falló = casi seguro spam
        if (callInfo.verificationStatus == VerificationStatus.FAILED) {
            return ScreeningResult(
                classification = CallClassification.BLOCK,
                reason = "Verificación de número falló",
                isColombian = numberInfo.isColombian
            )
        }

        // Solo bloquear con score muy alto
        if (riskScore > 0.9f) {
            return ScreeningResult(
                classification = CallClassification.BLOCK,
                reason = "Spam confirmado (confianza: ${(riskScore * 100).toInt()}%)",
                isColombian = numberInfo.isColombian,
                isKnownSpamPattern = true
            )
        }

        // Todo lo demás pasa
        return ScreeningResult(
            classification = CallClassification.ALLOW,
            reason = "Modo permisivo: llamada permitida",
            isColombian = numberInfo.isColombian
        )
    }
}
