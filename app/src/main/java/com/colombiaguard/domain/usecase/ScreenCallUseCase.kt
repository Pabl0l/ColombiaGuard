package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.CallClassification
import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.entity.CallPatternAnalyzer
import com.colombiaguard.domain.entity.ColombianNumberRanges
import com.colombiaguard.domain.entity.FilterConfig
import com.colombiaguard.domain.entity.FilterLevel
import com.colombiaguard.domain.entity.PhoneNumbers
import com.colombiaguard.domain.entity.ScreeningResult
import com.colombiaguard.domain.entity.VerificationStatus
import com.colombiaguard.domain.repository.ContactRepository
import com.colombiaguard.domain.repository.FilterConfigRepository
import com.colombiaguard.domain.repository.NumberListRepository

/**
 * Caso de uso principal: filtrar una llamada entrante.
 *
 * Orden de decisión (del más barato/al más caro):
 * 1. Normalizar el número (fuente única: [PhoneNumbers]).
 * 2. Emergencias → permitir siempre, incluso si están en la lista negra.
 * 3. Blacklist → bloquear. Whitelist → permitir.
 * 4. Contactos del usuario.
 * 5. Aplicar el nivel de filtro configurado con el score de riesgo.
 *
 * Principios: SRP (solo reglas de filtrado), DIP (depende de interfaces),
 * OCP (nuevas reglas sin tocar las existentes) y fail-safe (un error de
 * contactos se trata como "desconocido", nunca como bloqueo).
 */
class ScreenCallUseCase(
	private val contactRepository: ContactRepository,
	private val filterConfigRepository: FilterConfigRepository,
	private val numberListRepository: NumberListRepository
) {

	/**
	 * @param callInfo llamada entrante (cualquier formato de número)
	 * @param recentCalls historial reciente para detección de patrones
	 */
	suspend fun execute(
		callInfo: CallInfo,
		recentCalls: List<CallInfo> = emptyList()
	): ScreeningResult {
		// 1. Normalizar: a partir de aquí todo compara en dígitos canónicos.
		val normalized = PhoneNumbers.normalize(callInfo.phoneNumber)
		if (normalized.length < 3) {
			return ScreeningResult(CallClassification.ALLOW, "Número vacío o demasiado corto", isColombian = false)
		}
		val call = callInfo.copy(phoneNumber = normalized)

		// 2. Emergencias: siempre pasan.
		if (PhoneNumbers.isEmergency(normalized)) {
			return ScreeningResult(
				classification = CallClassification.ALLOW,
				reason = "Número de emergencia",
				isColombian = true,
				matchedLabel = "Emergencias"
			)
		}

		// 3. Listas del usuario.
		if (numberListRepository.isBlacklisted(normalized)) {
			return ScreeningResult(CallClassification.BLOCK, "Número en lista negra", isColombian = false)
		}
		if (numberListRepository.isWhitelisted(normalized)) {
			return ScreeningResult(CallClassification.WHITELISTED, "Número en lista blanca", isColombian = false)
		}

		// 4. Contactos (si falla el permiso se asume desconocido: fail-safe).
		val isContact = runCatching { contactRepository.isNumberInContacts(normalized) }.getOrDefault(false)

		val numberInfo = ColombianNumberRanges.analyzeNumber(normalized)
		val config = filterConfigRepository.getConfigOnce()

		// 5. Números especiales (líneas de atención) merecen pasar.
		if (numberInfo.isSpecial) {
			return ScreeningResult(
				classification = CallClassification.ALLOW,
				reason = "Número de emergencia/especial",
				isColombian = true,
				matchedLabel = numberInfo.label
			)
		}

		val risk = riskScore(call, numberInfo.isColombian, recentCalls, isContact, config)

		return when (config.level) {
			FilterLevel.STRICT -> applyStrictFilter(numberInfo, config, isContact, risk)
			FilterLevel.NORMAL -> applyNormalFilter(call, numberInfo, config, isContact, risk)
			FilterLevel.PERMISSIVE -> applyPermissiveFilter(call, numberInfo, risk)
		}
	}

	/**
	 * Score de riesgo 0..1: patrones + anti-spoofing + horario nocturno.
	 * Se calcula una sola vez y lo consumen los tres niveles.
	 */
	private fun riskScore(
		callInfo: CallInfo,
		isColombian: Boolean,
		recentCalls: List<CallInfo>,
		isContact: Boolean,
		config: FilterConfig
	): Float {
		var score = if (config.enablePatternDetection) {
			CallPatternAnalyzer.calculateRiskScore(CallPatternAnalyzer.analyzeCall(callInfo, recentCalls))
		} else {
			0f
		}
		// Anti-spoofing: la red dice que el número no es válido.
		if (callInfo.verificationStatus == VerificationStatus.FAILED && isColombian) {
			score += 0.3f
		}
		// Noche + desconocido: señal adicional.
		if (CallPatternAnalyzer.isOffHours(callInfo.timestamp) && !isContact) {
			score += 0.15f
		}
		return score.coerceAtMost(1f)
	}

	/** Estricto: solo números colombianos válidos; el resto se bloquea. */
	private fun applyStrictFilter(
		numberInfo: ColombianNumberRanges.NumberInfo,
		config: FilterConfig,
		isContact: Boolean,
		risk: Float
	): ScreeningResult {
		when {
			numberInfo.isForeign -> return ScreeningResult(
				CallClassification.BLOCK, "Número extranjero (modo estricto)", isColombian = false
			)
			!numberInfo.isColombian -> return ScreeningResult(
				CallClassification.BLOCK, "Número no reconocido como colombiano", isColombian = false
			)
			risk > 0.7f -> return ScreeningResult(
				CallClassification.BLOCK,
				"Patrón de spam detectado (riesgo ${(risk * 100).toInt()}%)",
				isColombian = true,
				isKnownSpamPattern = true,
				matchedLabel = numberInfo.label
			)
		}

		return if (isContact || config.whitelistAllContacts) {
			ScreeningResult(
				CallClassification.ALLOW,
				"Número colombiano válido (${numberInfo.label})",
				isColombian = true,
				matchedLabel = numberInfo.label
			)
		} else {
			ScreeningResult(
				CallClassification.SILENCE,
				"Número colombiano desconocido",
				isColombian = true,
				matchedLabel = numberInfo.label
			)
		}
	}

	/** Normal: contactos y colombianos válidos pasan; riesgo alto bloquea. */
	private fun applyNormalFilter(
		callInfo: CallInfo,
		numberInfo: ColombianNumberRanges.NumberInfo,
		config: FilterConfig,
		isContact: Boolean,
		risk: Float
	): ScreeningResult {
		if (callInfo.verificationStatus == VerificationStatus.FAILED) {
			return ScreeningResult(CallClassification.BLOCK, "Verificación de número falló", numberInfo.isColombian)
		}
		if (isContact) {
			return ScreeningResult(CallClassification.ALLOW, "Contacto del usuario", numberInfo.isColombian)
		}
		if (numberInfo.isForeign) {
			return if (config.blockForeignCalls) {
				ScreeningResult(CallClassification.BLOCK, "Llamada extranjera bloqueada", isColombian = false)
			} else {
				ScreeningResult(CallClassification.SILENCE, "Llamada extranjera silenciada", isColombian = false)
			}
		}
		if (!numberInfo.isColombian) {
			return ScreeningResult(CallClassification.SILENCE, "Número no reconocido", isColombian = false)
		}

		return when {
			risk > 0.8f -> ScreeningResult(
				CallClassification.BLOCK,
				"Alto riesgo de spam (${(risk * 100).toInt()}%)",
				isColombian = true,
				isKnownSpamPattern = true
			)
			risk > 0.5f -> ScreeningResult(
				CallClassification.SILENCE, "Riesgo moderado de spam", isColombian = true
			)
			config.silenceUnknownColombian -> ScreeningResult(
				CallClassification.SILENCE, "Número colombiano desconocido", isColombian = true
			)
			else -> ScreeningResult(
				CallClassification.ALLOW,
				"Número colombiano válido (${numberInfo.label})",
				isColombian = true,
				matchedLabel = numberInfo.label
			)
		}
	}

	/** Permisivo: solo bloquea spam con confianza muy alta. */
	private fun applyPermissiveFilter(
		callInfo: CallInfo,
		numberInfo: ColombianNumberRanges.NumberInfo,
		risk: Float
	): ScreeningResult = when {
		// La red dice que el número no es válido: casi seguro spoofing.
		callInfo.verificationStatus == VerificationStatus.FAILED -> ScreeningResult(
			CallClassification.BLOCK, "Verificación de número falló", numberInfo.isColombian
		)
		risk > 0.9f -> ScreeningResult(
			CallClassification.BLOCK,
			"Spam confirmado (confianza ${(risk * 100).toInt()}%)",
			isColombian = numberInfo.isColombian,
			isKnownSpamPattern = true
		)
		else -> ScreeningResult(
			CallClassification.ALLOW, "Modo permisivo: llamada permitida", isColombian = numberInfo.isColombian
		)
	}
}
