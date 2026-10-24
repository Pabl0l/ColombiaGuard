package com.colombiaguard.domain.repository

import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.entity.ScreeningResult
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para el historial de llamadas analizadas.
 * SRP: solo maneja persistencia de llamadas.
 */
interface CallHistoryRepository {

	/**
	 * Registra una llamada con el resultado del screening, de modo que las
	 * estadísticas (bloqueadas / extranjeras / spam) se deriven del resultado
	 * real y no de valores inventados en el repositorio.
	 */
	suspend fun logCall(callInfo: CallInfo, result: ScreeningResult)

	/** Obtiene las llamadas de las últimas [hoursBack] horas. */
	suspend fun getRecentCalls(hoursBack: Int = 24): List<CallInfo>

	fun getBlockedCalls(): Flow<List<CallLogEntry>>

	fun getAllowedCalls(): Flow<List<CallLogEntry>>

	suspend fun getStats(): CallStats

	/** Elimina registros anteriores a [hoursBack] horas (retención). */
	suspend fun purgeOlderThan(hoursBack: Int = DEFAULT_RETENTION_HOURS)

	companion object {
		/** Retención por defecto del historial: 30 días. */
		const val DEFAULT_RETENTION_HOURS = 24 * 30
	}
}

data class CallLogEntry(
	val phoneNumber: String,
	val timestamp: Long,
	val wasBlocked: Boolean,
	val reason: String,
	val isColombian: Boolean
)

data class CallStats(
	val totalCalls: Int,
	val blockedCalls: Int,
	val allowedCalls: Int,
	val foreignBlocked: Int,
	val spamBlocked: Int
)
