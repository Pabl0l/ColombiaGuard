package com.colombiaguard.fakes

import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.entity.FilterConfig
import com.colombiaguard.domain.entity.PhoneNumbers
import com.colombiaguard.domain.entity.ScreeningResult
import com.colombiaguard.domain.repository.CallHistoryRepository
import com.colombiaguard.domain.repository.CallLogEntry
import com.colombiaguard.domain.repository.CallStats
import com.colombiaguard.domain.repository.ContactRepository
import com.colombiaguard.domain.repository.FilterConfigRepository
import com.colombiaguard.domain.repository.ListedNumber
import com.colombiaguard.domain.repository.NumberListRepository
import com.colombiaguard.domain.repository.NumberSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone

/** Contactos en memoria; [failWithPermissionError] simula READ_CONTACTS denegado. */
class FakeContactRepository(
	private val contacts: Set<String> = emptySet(),
	private val failWithPermissionError: Boolean = false
) : ContactRepository {

	override suspend fun isNumberInContacts(phoneNumber: String): Boolean {
		if (failWithPermissionError) throw SecurityException("READ_CONTACTS denegado")
		return PhoneNumbers.normalize(phoneNumber) in contacts.map { PhoneNumbers.normalize(it) }
	}

	override suspend fun getAllContactNumbers(): Set<String> = contacts
}

/** Configuración en memoria con Flow reactivo. */
class FakeFilterConfigRepository(var config: FilterConfig = FilterConfig()) : FilterConfigRepository {

	private val flow = MutableStateFlow(config)

	override fun getConfig(): Flow<FilterConfig> = flow

	override suspend fun getConfigOnce(): FilterConfig = config

	override suspend fun updateConfig(config: FilterConfig) {
		this.config = config
		flow.value = config
	}
}

/** Listas en memoria. Normaliza igual que la implementación real. */
class FakeNumberListRepository(
	whitelist: Set<String> = emptySet(),
	blacklist: Set<String> = emptySet()
) : NumberListRepository {

	private val white = whitelist.map { PhoneNumbers.normalize(it) }.toMutableSet()
	private val black = blacklist.map { PhoneNumbers.normalize(it) }.toMutableSet()
	private val labels = mutableMapOf<String, String>()
	private val whiteFlow = MutableStateFlow<List<ListedNumber>>(emptyList())
	private val blackFlow = MutableStateFlow<List<ListedNumber>>(emptyList())

	override suspend fun isWhitelisted(phoneNumber: String) = PhoneNumbers.normalize(phoneNumber) in white

	override suspend fun isBlacklisted(phoneNumber: String) = PhoneNumbers.normalize(phoneNumber) in black

	override suspend fun addToWhitelist(phoneNumber: String, label: String) {
		val normalized = PhoneNumbers.normalize(phoneNumber)
		white += normalized
		labels[normalized] = label
		whiteFlow.value = white.map { listed(it, labels[it].orEmpty()) }
	}

	override suspend fun addToBlacklist(phoneNumber: String, label: String) {
		val normalized = PhoneNumbers.normalize(phoneNumber)
		black += normalized
		labels[normalized] = label
		blackFlow.value = black.map { listed(it, labels[it].orEmpty()) }
	}

	override suspend fun removeFromWhitelist(phoneNumber: String) {
		white -= PhoneNumbers.normalize(phoneNumber)
		whiteFlow.value = white.map { listed(it, labels[it].orEmpty()) }
	}

	override suspend fun removeFromBlacklist(phoneNumber: String) {
		black -= PhoneNumbers.normalize(phoneNumber)
		blackFlow.value = black.map { listed(it, labels[it].orEmpty()) }
	}

	override fun getWhitelistedNumbers(): Flow<List<ListedNumber>> = whiteFlow

	override fun getBlacklistedNumbers(): Flow<List<ListedNumber>> = blackFlow

	private fun listed(number: String, label: String) =
		ListedNumber(number, label, addedAt = 0L, source = NumberSource.MANUAL)
}

/** Historial en memoria para pruebas de casos de uso. */
class FakeCallHistoryRepository(
	private val recent: List<CallInfo> = emptyList()
) : CallHistoryRepository {

	val logged = mutableListOf<Pair<CallInfo, ScreeningResult>>()
	var lastPurgeHours: Int? = null
	var lastRecentHours: Int? = null

	override suspend fun logCall(callInfo: CallInfo, result: ScreeningResult) {
		logged += callInfo to result
	}

	override suspend fun getRecentCalls(hoursBack: Int): List<CallInfo> {
		lastRecentHours = hoursBack
		return recent
	}

	override fun getBlockedCalls(): Flow<List<CallLogEntry>> = flowOf(emptyList())

	override fun getAllowedCalls(): Flow<List<CallLogEntry>> = flowOf(emptyList())

	override suspend fun getStats(): CallStats = CallStats(0, 0, 0, 0, 0)

	override suspend fun purgeOlderThan(hoursBack: Int) {
		lastPurgeHours = hoursBack
	}
}

/** Timestamp en hora de Colombia, para no depender de la zona del runner. */
fun bogotaTimestamp(hour: Int, minute: Int = 0, dayOffset: Int = 0): Long =
	Calendar.getInstance(TimeZone.getTimeZone("America/Bogota")).apply {
		set(2026, Calendar.JANUARY, 15, hour, minute, 0)
		set(Calendar.MILLISECOND, 0)
		add(Calendar.DAY_OF_MONTH, dayOffset)
	}.timeInMillis
