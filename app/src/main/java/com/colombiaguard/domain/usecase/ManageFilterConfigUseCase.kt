package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.FilterConfig
import com.colombiaguard.domain.entity.FilterLevel
import com.colombiaguard.domain.repository.FilterConfigRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para gestionar la configuración del filtro.
 * Principio SRP: Solo maneja configuración.
 */
class ManageFilterConfigUseCase(
    private val filterConfigRepository: FilterConfigRepository
) {

    /**
     * Observa cambios en la configuración.
     */
    fun observeConfig(): Flow<FilterConfig> {
        return filterConfigRepository.getConfig()
    }

    /**
     * Obtiene la configuración actual.
     */
    suspend fun getCurrentConfig(): FilterConfig {
        return filterConfigRepository.getConfigOnce()
    }

    /**
     * Cambia el nivel del filtro.
     */
    suspend fun setFilterLevel(level: FilterLevel) {
        val current = filterConfigRepository.getConfigOnce()
        filterConfigRepository.updateConfig(current.copy(level = level))
    }

    /**
     * Activa/desactiva bloqueo de llamadas extranjeras.
     */
    suspend fun setBlockForeign(block: Boolean) {
        val current = filterConfigRepository.getConfigOnce()
        filterConfigRepository.updateConfig(current.copy(blockForeignCalls = block))
    }

    /**
     * Activa/desactiva silenciamiento de colombianos desconocidos.
     */
    suspend fun setSilenceUnknown(silence: Boolean) {
        val current = filterConfigRepository.getConfigOnce()
        filterConfigRepository.updateConfig(current.copy(silenceUnknownColombian = silence))
    }

    /**
     * Activa/desactiva detección de patrones.
     */
    suspend fun setPatternDetection(enabled: Boolean) {
        val current = filterConfigRepository.getConfigOnce()
        filterConfigRepository.updateConfig(current.copy(enablePatternDetection = enabled))
    }

    /**
     * Resetea la configuración a valores por defecto.
     */
    suspend fun resetToDefaults() {
        filterConfigRepository.updateConfig(FilterConfig())
    }
}
