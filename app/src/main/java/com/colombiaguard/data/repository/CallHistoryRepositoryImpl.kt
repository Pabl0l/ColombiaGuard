package com.colombiaguard.data.repository

import com.colombiaguard.data.local.CallLogDao
import com.colombiaguard.data.local.CallLogEntity
import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.repository.CallHistoryRepository
import com.colombiaguard.domain.repository.CallLogEntry
import com.colombiaguard.domain.repository.CallStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de CallHistoryRepository usando Room.
 * Principio SRP: Solo persiste historial de llamadas.
 */
class CallHistoryRepositoryImpl(
    private val callLogDao: CallLogDao
) : CallHistoryRepository {

    override suspend fun logCall(callInfo: CallInfo, wasBlocked: Boolean, reason: String) {
        val entity = CallLogEntity(
            phoneNumber = callInfo.phoneNumber,
            timestamp = callInfo.timestamp,
            wasBlocked = wasBlocked,
            reason = reason,
            isColombian = false, // Se puede calcular si se necesita
            isFromContacts = callInfo.isFromContacts
        )
        callLogDao.insertCall(entity)
    }

    override suspend fun getRecentCalls(hoursBack: Int): List<CallInfo> {
        val since = System.currentTimeMillis() - (hoursBack * 60 * 60 * 1000L)
        return callLogDao.getCallsSince(since).map { entity ->
            CallInfo(
                phoneNumber = entity.phoneNumber,
                timestamp = entity.timestamp,
                isFromContacts = entity.isFromContacts
            )
        }
    }

    override fun getBlockedCalls(): Flow<List<CallLogEntry>> {
        return callLogDao.getBlockedCalls().map { entities ->
            entities.map { it.toLogEntry() }
        }
    }

    override fun getAllowedCalls(): Flow<List<CallLogEntry>> {
        return callLogDao.getAllowedCalls().map { entities ->
            entities.map { it.toLogEntry() }
        }
    }

    override suspend fun getStats(): CallStats {
        return CallStats(
            totalCalls = callLogDao.getTotalCalls(),
            blockedCalls = callLogDao.getBlockedCount(),
            allowedCalls = callLogDao.getAllowedCount(),
            foreignBlocked = callLogDao.getForeignBlockedCount(),
            spamBlocked = callLogDao.getSpamBlockedCount()
        )
    }

    private fun CallLogEntity.toLogEntry() = CallLogEntry(
        phoneNumber = phoneNumber,
        timestamp = timestamp,
        wasBlocked = wasBlocked,
        reason = reason,
        isColombian = isColombian
    )
}
