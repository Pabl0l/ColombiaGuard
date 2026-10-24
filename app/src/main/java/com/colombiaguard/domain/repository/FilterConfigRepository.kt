package com.colombiaguard.domain.repository

import com.colombiaguard.domain.entity.FilterConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para la configuración del filtro.
 * Principio SRP: Solo maneja persistencia de configuración.
 */
interface FilterConfigRepository {
    /**
     * Obtiene la configuración actual como Flow reactivo.
     */
    fun getConfig(): Flow<FilterConfig>

    /**
     * Actualiza la configuración del filtro.
     */
    suspend fun updateConfig(config: FilterConfig)

    /**
     * Obtiene la configuración actual (una sola vez).
     */
    suspend fun getConfigOnce(): FilterConfig
}
