package com.colombiaguard.data.repository

import com.colombiaguard.data.local.NumberListDao
import com.colombiaguard.data.local.NumberListEntity
import com.colombiaguard.domain.entity.PhoneNumbers
import com.colombiaguard.domain.repository.ListedNumber
import com.colombiaguard.domain.repository.NumberListRepository
import com.colombiaguard.domain.repository.NumberSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de NumberListRepository usando Room.
 *
 * Toda entrada y toda consulta pasan por [PhoneNumbers.normalize]: es la única
 * forma de que "+57 300 123 4567" (lo que escribe el usuario) y
 * "+573001234567" (lo que entrega la red) se consideren el mismo número.
 */
class NumberListRepositoryImpl(
	private val numberListDao: NumberListDao
) : NumberListRepository {

	override suspend fun isWhitelisted(phoneNumber: String): Boolean =
		numberListDao.isWhitelisted(PhoneNumbers.normalize(phoneNumber))

	override suspend fun isBlacklisted(phoneNumber: String): Boolean =
		numberListDao.isBlacklisted(PhoneNumbers.normalize(phoneNumber))

	override suspend fun addToWhitelist(phoneNumber: String, label: String) {
		numberListDao.insertNumber(entity(phoneNumber, label, isWhitelisted = true))
	}

	override suspend fun addToBlacklist(phoneNumber: String, label: String) {
		numberListDao.insertNumber(entity(phoneNumber, label, isWhitelisted = false))
	}

	override suspend fun removeFromWhitelist(phoneNumber: String) {
		numberListDao.deleteByNumber(PhoneNumbers.normalize(phoneNumber), isWhitelisted = true)
	}

	override suspend fun removeFromBlacklist(phoneNumber: String) {
		numberListDao.deleteByNumber(PhoneNumbers.normalize(phoneNumber), isWhitelisted = false)
	}

	override fun getWhitelistedNumbers(): Flow<List<ListedNumber>> =
		numberListDao.getWhitelistedNumbers().map { entities -> entities.map { it.toListedNumber() } }

	override fun getBlacklistedNumbers(): Flow<List<ListedNumber>> =
		numberListDao.getBlacklistedNumbers().map { entities -> entities.map { it.toListedNumber() } }

	private fun entity(phoneNumber: String, label: String, isWhitelisted: Boolean) = NumberListEntity(
		phoneNumber = PhoneNumbers.normalize(phoneNumber),
		label = label,
		addedAt = System.currentTimeMillis(),
		isWhitelisted = isWhitelisted,
		source = NumberSource.MANUAL.name
	)

	private fun NumberListEntity.toListedNumber() = ListedNumber(
		phoneNumber = phoneNumber,
		label = label,
		addedAt = addedAt,
		source = runCatching { NumberSource.valueOf(source) }.getOrDefault(NumberSource.MANUAL)
	)
}
