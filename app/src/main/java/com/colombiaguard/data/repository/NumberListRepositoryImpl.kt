package com.colombiaguard.data.repository

import com.colombiaguard.data.local.NumberListDao
import com.colombiaguard.data.local.NumberListEntity
import com.colombiaguard.domain.repository.ListedNumber
import com.colombiaguard.domain.repository.NumberListRepository
import com.colombiaguard.domain.repository.NumberSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de NumberListRepository usando Room.
 * Principio SRP: Solo persiste listas de números.
 */
class NumberListRepositoryImpl(
    private val numberListDao: NumberListDao
) : NumberListRepository {

    override suspend fun isWhitelisted(phoneNumber: String): Boolean {
        return numberListDao.isWhitelisted(phoneNumber)
    }

    override suspend fun isBlacklisted(phoneNumber: String): Boolean {
        return numberListDao.isBlacklisted(phoneNumber)
    }

    override suspend fun addToWhitelist(phoneNumber: String, label: String) {
        val entity = NumberListEntity(
            phoneNumber = phoneNumber,
            label = label,
            addedAt = System.currentTimeMillis(),
            isWhitelisted = true,
            source = NumberSource.MANUAL.name
        )
        numberListDao.insertNumber(entity)
    }

    override suspend fun addToBlacklist(phoneNumber: String, label: String) {
        val entity = NumberListEntity(
            phoneNumber = phoneNumber,
            label = label,
            addedAt = System.currentTimeMillis(),
            isWhitelisted = false,
            source = NumberSource.MANUAL.name
        )
        numberListDao.insertNumber(entity)
    }

    override suspend fun removeFromWhitelist(phoneNumber: String) {
        numberListDao.deleteByNumber(phoneNumber, isWhitelisted = true)
    }

    override suspend fun removeFromBlacklist(phoneNumber: String) {
        numberListDao.deleteByNumber(phoneNumber, isWhitelisted = false)
    }

    override fun getWhitelistedNumbers(): Flow<List<ListedNumber>> {
        return numberListDao.getWhitelistedNumbers().map { entities ->
            entities.map { it.toListedNumber() }
        }
    }

    override fun getBlacklistedNumbers(): Flow<List<ListedNumber>> {
        return numberListDao.getBlacklistedNumbers().map { entities ->
            entities.map { it.toListedNumber() }
        }
    }

    private fun NumberListEntity.toListedNumber() = ListedNumber(
        phoneNumber = phoneNumber,
        label = label,
        addedAt = addedAt,
        source = try {
            NumberSource.valueOf(source)
        } catch (e: Exception) {
            NumberSource.MANUAL
        }
    )
}
