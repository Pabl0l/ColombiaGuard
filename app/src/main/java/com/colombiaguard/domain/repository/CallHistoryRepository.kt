package com.colombiaguard.domain.repository

import com.colombiaguard.domain.entity.CallInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para el historial de llamadas analizadas.
 * Principio SRP: Solo maneja persistencia de llamadas.
 */
interface CallHistoryRepository {
    /**
     * Registra una llamada analizada.
     */
    suspend fun logCall(callInfo: CallInfo, wasBlocked: Boolean, reason: String)

    /**
     * Obtiene las llamadas recientes (últimas 24h).
     */
    suspend fun getRecentCalls(hoursBack: Int = 24): List<CallInfo>

    /**
     * Obtiene el historial de llamadas bloqueadas como Flow.
     */
    fun getBlockedCalls(): Flow<List<CallLogEntry>>

    /**
     * Obtiene el historial de llamadas permitidas como Flow.
     */
    fun getAllowedCalls(): Flow<List<CallLogEntry>>

    /**
     * Obtiene estadísticas de bloqueo.
     */
    suspend fun getStats(): CallStats
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
