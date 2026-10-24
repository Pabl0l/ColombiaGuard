package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.repository.CallHistoryRepository
import com.colombiaguard.domain.repository.CallStats

/**
 * Caso de uso para gestionar el historial de llamadas.
 * Principio SRP: Solo maneja operaciones de historial.
 */
class ManageCallHistoryUseCase(
    private val callHistoryRepository: CallHistoryRepository
) {

    /**
     * Registra una llamada analizada.
     */
    suspend fun logCall(callInfo: CallInfo, wasBlocked: Boolean, reason: String) {
        callHistoryRepository.logCall(callInfo, wasBlocked, reason)
    }

    /**
     * Obtiene llamadas recientes para análisis de patrones.
     */
    suspend fun getRecentCalls(hoursBack: Int = 24): List<CallInfo> {
        return callHistoryRepository.getRecentCalls(hoursBack)
    }

    /**
     * Obtiene estadísticas de la app.
     */
    suspend fun getStats(): CallStats {
        return callHistoryRepository.getStats()
    }
}
