package com.colombiaguard.domain.repository

/**
 * Repositorio para la lista blanca/negra de números.
 * Principio SRP: Solo maneja whitelist/blacklist.
 */
interface NumberListRepository {
    /**
     * Verifica si un número está en la whitelist.
     */
    suspend fun isWhitelisted(phoneNumber: String): Boolean

    /**
     * Verifica si un número está en la blacklist.
     */
    suspend fun isBlacklisted(phoneNumber: String): Boolean

    /**
     * Agrega un número a la whitelist.
     */
    suspend fun addToWhitelist(phoneNumber: String, label: String = "")

    /**
     * Agrega un número a la blacklist.
     */
    suspend fun addToBlacklist(phoneNumber: String, label: String = "")

    /**
     * Remueve un número de la whitelist.
     */
    suspend fun removeFromWhitelist(phoneNumber: String)

    /**
     * Remueve un número de la blacklist.
     */
    suspend fun removeFromBlacklist(phoneNumber: String)

    /**
     * Obtiene todos los números en la whitelist.
     */
    fun getWhitelistedNumbers(): kotlinx.coroutines.flow.Flow<List<ListedNumber>>

    /**
     * Obtiene todos los números en la blacklist.
     */
    fun getBlacklistedNumbers(): kotlinx.coroutines.flow.Flow<List<ListedNumber>>
}

data class ListedNumber(
    val phoneNumber: String,
    val label: String,
    val addedAt: Long,
    val source: NumberSource
)

enum class NumberSource {
    /** Agregado manualmente por el usuario */
    MANUAL,
    /** Agregado automáticamente desde contactos */
    CONTACTS,
    /** Agregado automáticamente por el sistema */
    SYSTEM
}
