package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.entity.ScreeningResult
import com.colombiaguard.domain.repository.CallHistoryRepository
import com.colombiaguard.domain.repository.CallStats

/**
 * Caso de uso para gestionar el historial de llamadas.
 * SRP: solo maneja operaciones de historial.
 */
class ManageCallHistoryUseCase(
	private val callHistoryRepository: CallHistoryRepository
) {

	/** Registra una llamada con el resultado real del screening. */
	suspend fun logCall(callInfo: CallInfo, result: ScreeningResult) {
		callHistoryRepository.logCall(callInfo, result)
	}

	/** Llamadas recientes, usadas para la detección de patrones. */
	suspend fun getRecentCalls(hoursBack: Int = 24): List<CallInfo> =
		callHistoryRepository.getRecentCalls(hoursBack)

	/** Estadísticas agregadas para el dashboard. */
	suspend fun getStats(): CallStats = callHistoryRepository.getStats()

	/** Aplica la retención del historial (30 días por defecto). */
	suspend fun purgeOldHistory(hoursBack: Int = CallHistoryRepository.DEFAULT_RETENTION_HOURS) {
		callHistoryRepository.purgeOlderThan(hoursBack)
	}
}
