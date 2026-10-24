package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.FilterConfig
import com.colombiaguard.domain.entity.FilterLevel
import com.colombiaguard.domain.repository.FilterConfigRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para gestionar la configuración del filtro.
 * SRP: solo maneja configuración.
 */
class ManageFilterConfigUseCase(
	private val filterConfigRepository: FilterConfigRepository
) {

	fun observeConfig(): Flow<FilterConfig> = filterConfigRepository.getConfig()

	suspend fun getCurrentConfig(): FilterConfig = filterConfigRepository.getConfigOnce()

	suspend fun setFilterLevel(level: FilterLevel) =
		update { it.copy(level = level) }

	suspend fun setBlockForeign(block: Boolean) =
		update { it.copy(blockForeignCalls = block) }

	suspend fun setSilenceUnknown(silence: Boolean) =
		update { it.copy(silenceUnknownColombian = silence) }

	suspend fun setPatternDetection(enabled: Boolean) =
		update { it.copy(enablePatternDetection = enabled) }

	suspend fun setWhitelistContacts(enabled: Boolean) =
		update { it.copy(whitelistAllContacts = enabled) }

	suspend fun resetToDefaults() {
		filterConfigRepository.updateConfig(FilterConfig())
	}

	private suspend fun update(transform: (FilterConfig) -> FilterConfig) {
		filterConfigRepository.updateConfig(transform(filterConfigRepository.getConfigOnce()))
	}
}
