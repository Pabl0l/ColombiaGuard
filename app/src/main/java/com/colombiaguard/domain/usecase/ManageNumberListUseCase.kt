package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.ColombianNumberRanges
import com.colombiaguard.domain.entity.PhoneNumbers
import com.colombiaguard.domain.repository.ListedNumber
import com.colombiaguard.domain.repository.NumberListRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para gestionar whitelist y blacklist.
 * SRP: solo maneja listas de números.
 *
 * Normaliza y valida en la frontera: lo que entra a la BD siempre son dígitos
 * canónicos, que es lo que después compara el servicio de screening.
 */
class ManageNumberListUseCase(
	private val numberListRepository: NumberListRepository
) {

	suspend fun isAllowed(phoneNumber: String): Boolean =
		numberListRepository.isWhitelisted(phoneNumber)

	suspend fun isBlocked(phoneNumber: String): Boolean =
		numberListRepository.isBlacklisted(phoneNumber)

	/**
	 * Agrega a la whitelist.
	 * @return `false` si el número no tiene un formato plausible.
	 */
	suspend fun allowNumber(phoneNumber: String, label: String = ""): Boolean {
		if (!PhoneNumbers.hasValidFormat(phoneNumber)) return false
		numberListRepository.addToWhitelist(phoneNumber, label.ifBlank { defaultLabel(phoneNumber) })
		return true
	}

	/**
	 * Agrega a la blacklist.
	 * @return `false` si el número no tiene un formato plausible.
	 */
	suspend fun blockNumber(phoneNumber: String, label: String = ""): Boolean {
		if (!PhoneNumbers.hasValidFormat(phoneNumber)) return false
		numberListRepository.addToBlacklist(phoneNumber, label.ifBlank { defaultLabel(phoneNumber) })
		return true
	}

	suspend fun removeFromAllow(phoneNumber: String) {
		numberListRepository.removeFromWhitelist(phoneNumber)
	}

	suspend fun removeFromBlock(phoneNumber: String) {
		numberListRepository.removeFromBlacklist(phoneNumber)
	}

	fun observeWhitelist(): Flow<List<ListedNumber>> = numberListRepository.getWhitelistedNumbers()

	fun observeBlacklist(): Flow<List<ListedNumber>> = numberListRepository.getBlacklistedNumbers()

	private fun defaultLabel(phoneNumber: String): String =
		ColombianNumberRanges.analyzeNumber(phoneNumber).label
}
