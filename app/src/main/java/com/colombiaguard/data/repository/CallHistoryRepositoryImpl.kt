package com.colombiaguard.data.repository

import com.colombiaguard.data.local.CallLogDao
import com.colombiaguard.data.local.CallLogEntity
import com.colombiaguard.domain.entity.CallClassification
import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.entity.PhoneNumbers
import com.colombiaguard.domain.entity.ScreeningResult
import com.colombiaguard.domain.repository.CallHistoryRepository
import com.colombiaguard.domain.repository.CallLogEntry
import com.colombiaguard.domain.repository.CallStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de CallHistoryRepository usando Room.
 * SRP: solo persiste historial de llamadas.
 */
class CallHistoryRepositoryImpl(
	private val callLogDao: CallLogDao
) : CallHistoryRepository {

	override suspend fun logCall(callInfo: CallInfo, result: ScreeningResult) {
		callLogDao.insertCall(
			CallLogEntity(
				phoneNumber = PhoneNumbers.normalize(callInfo.phoneNumber),
				timestamp = callInfo.timestamp,
				wasBlocked = result.classification == CallClassification.BLOCK,
				reason = result.reason,
				isColombian = result.isColombian,
				isFromContacts = callInfo.isFromContacts
			)
		)
	}

	override suspend fun getRecentCalls(hoursBack: Int): List<CallInfo> {
		val since = System.currentTimeMillis() - hoursBack * HOUR_MS
		return callLogDao.getCallsSince(since).map { entity ->
			CallInfo(
				phoneNumber = entity.phoneNumber,
				timestamp = entity.timestamp,
				isFromContacts = entity.isFromContacts
			)
		}
	}

	override fun getBlockedCalls(): Flow<List<CallLogEntry>> =
		callLogDao.getBlockedCalls().map { entities -> entities.map { it.toLogEntry() } }

	override fun getAllowedCalls(): Flow<List<CallLogEntry>> =
		callLogDao.getAllowedCalls().map { entities -> entities.map { it.toLogEntry() } }

	override suspend fun getStats(): CallStats = CallStats(
		totalCalls = callLogDao.getTotalCalls(),
		blockedCalls = callLogDao.getBlockedCount(),
		allowedCalls = callLogDao.getAllowedCount(),
		foreignBlocked = callLogDao.getForeignBlockedCount(),
		spamBlocked = callLogDao.getSpamBlockedCount()
	)

	override suspend fun purgeOlderThan(hoursBack: Int) {
		callLogDao.deleteOlderThan(System.currentTimeMillis() - hoursBack * HOUR_MS)
	}

	private fun CallLogEntity.toLogEntry() = CallLogEntry(
		phoneNumber = phoneNumber,
		timestamp = timestamp,
		wasBlocked = wasBlocked,
		reason = reason,
		isColombian = isColombian
	)

	private companion object {
		const val HOUR_MS = 60 * 60 * 1000L
	}
}
