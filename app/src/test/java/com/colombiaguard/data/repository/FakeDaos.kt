package com.colombiaguard.data.repository

import com.colombiaguard.data.local.CallLogDao
import com.colombiaguard.data.local.CallLogEntity
import com.colombiaguard.data.local.NumberListDao
import com.colombiaguard.data.local.NumberListEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO de listas en memoria con la misma semántica que la consulta SQL. */
class FakeNumberListDao : NumberListDao {

	private val rows = linkedMapOf<String, NumberListEntity>()
	private val state = MutableStateFlow<List<NumberListEntity>>(emptyList())

	override suspend fun insertNumber(number: NumberListEntity) {
		rows[number.phoneNumber] = number
		publish()
	}

	override suspend fun deleteNumber(number: NumberListEntity) {
		rows.remove(number.phoneNumber)
		publish()
	}

	override suspend fun deleteByNumber(phoneNumber: String, isWhitelisted: Boolean) {
		rows[phoneNumber]?.takeIf { it.isWhitelisted == isWhitelisted }?.let { rows.remove(phoneNumber) }
		publish()
	}

	override suspend fun isWhitelisted(phoneNumber: String): Boolean =
		rows[phoneNumber]?.isWhitelisted == true

	override suspend fun isBlacklisted(phoneNumber: String): Boolean =
		rows[phoneNumber]?.isWhitelisted == false

	override fun getWhitelistedNumbers(): Flow<List<NumberListEntity>> =
		state.map { list -> list.filter { it.isWhitelisted } }

	override fun getBlacklistedNumbers(): Flow<List<NumberListEntity>> =
		state.map { list -> list.filter { !it.isWhitelisted } }

	override suspend fun getAllWhitelisted(): List<NumberListEntity> = rows.values.filter { it.isWhitelisted }

	private fun publish() {
		state.value = rows.values.toList()
	}
}

/** DAO de historial en memoria. */
class FakeCallLogDao : CallLogDao {

	val inserted = mutableListOf<CallLogEntity>()
	var lastDeleteOlderThan: Long? = null
	private var nextId = 1L

	override suspend fun insertCall(call: CallLogEntity): Long {
		inserted += call
		return nextId++
	}

	override suspend fun getCallsSince(since: Long): List<CallLogEntity> =
		inserted.filter { it.timestamp > since }

	override fun getBlockedCalls(limit: Int): Flow<List<CallLogEntity>> =
		MutableStateFlow(inserted.filter { it.wasBlocked }.take(limit))

	override fun getAllowedCalls(limit: Int): Flow<List<CallLogEntity>> =
		MutableStateFlow(inserted.filter { !it.wasBlocked }.take(limit))

	override suspend fun getTotalCalls(): Int = inserted.size

	override suspend fun getBlockedCount(): Int = inserted.count { it.wasBlocked }

	override suspend fun getAllowedCount(): Int = inserted.count { !it.wasBlocked }

	override suspend fun getForeignBlockedCount(): Int = inserted.count { it.wasBlocked && !it.isColombian }

	override suspend fun getSpamBlockedCount(): Int =
		inserted.count { it.wasBlocked && it.reason.contains("spam", ignoreCase = true) }

	override suspend fun deleteOlderThan(before: Long) {
		lastDeleteOlderThan = before
		inserted.removeAll { it.timestamp < before }
	}

	override suspend fun getCallsByNumber(number: String): List<CallLogEntity> =
		inserted.filter { it.phoneNumber == number }
}
