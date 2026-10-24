package com.colombiaguard.domain.repository

import com.colombiaguard.domain.entity.FilterConfig
import com.colombiaguard.domain.entity.CallInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para acceder a contactos del dispositivo.
 * Principio SRP: Solo maneja operaciones de contactos.
 */
interface ContactRepository {
    /**
     * Verifica si un número está en los contactos del usuario.
     */
    suspend fun isNumberInContacts(phoneNumber: String): Boolean

    /**
     * Obtiene todos los números de contactos normalizados.
     */
    suspend fun getAllContactNumbers(): Set<String>
}
