package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.ColombianNumberRanges
import com.colombiaguard.domain.repository.NumberListRepository
import com.colombiaguard.domain.repository.ListedNumber
import com.colombiaguard.domain.repository.NumberSource
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para gestionar whitelist y blacklist.
 * Principio SRP: Solo maneja listas de números.
 */
class ManageNumberListUseCase(
    private val numberListRepository: NumberListRepository
) {

    /**
     * Verifica si un número está permitido (whitelist).
     */
    suspend fun isAllowed(phoneNumber: String): Boolean {
        return numberListRepository.isWhitelisted(phoneNumber)
    }

    /**
     * Verifica si un número está bloqueado (blacklist).
     */
    suspend fun isBlocked(phoneNumber: String): Boolean {
        return numberListRepository.isBlacklisted(phoneNumber)
    }

    /**
     * Agrega un número a la whitelist.
     */
    suspend fun allowNumber(phoneNumber: String, label: String = "") {
        val info = ColombianNumberRanges.analyzeNumber(phoneNumber)
        val autoLabel = label.ifEmpty { info.operator ?: "Número permitido" }
        numberListRepository.addToWhitelist(phoneNumber, autoLabel)
    }

    /**
     * Agrega un número a la blacklist.
     */
    suspend fun blockNumber(phoneNumber: String, label: String = "") {
        val info = ColombianNumberRanges.analyzeNumber(phoneNumber)
        val autoLabel = label.ifEmpty { info.operator ?: "Número bloqueado" }
        numberListRepository.addToBlacklist(phoneNumber, autoLabel)
    }

    /**
     * Remueve un número de la whitelist.
     */
    suspend fun removeFromAllow(phoneNumber: String) {
        numberListRepository.removeFromWhitelist(phoneNumber)
    }

    /**
     * Remueve un número de la blacklist.
     */
    suspend fun removeFromBlock(phoneNumber: String) {
        numberListRepository.removeFromBlacklist(phoneNumber)
    }

    /**
     * Observa la whitelist.
     */
    fun observeWhitelist(): Flow<List<ListedNumber>> {
        return numberListRepository.getWhitelistedNumbers()
    }

    /**
     * Observa la blacklist.
     */
    fun observeBlacklist(): Flow<List<ListedNumber>> {
        return numberListRepository.getBlacklistedNumbers()
    }
}
