package com.colombiaguard.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de whitelist/blacklist.
 * Principio SRP: Solo opera sobre number_list.
 */
@Dao
interface NumberListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNumber(number: NumberListEntity)

    @Delete
    suspend fun deleteNumber(number: NumberListEntity)

    @Query("DELETE FROM number_list WHERE phoneNumber = :phoneNumber AND isWhitelisted = :isWhitelisted")
    suspend fun deleteByNumber(phoneNumber: String, isWhitelisted: Boolean)

    @Query("SELECT EXISTS(SELECT 1 FROM number_list WHERE phoneNumber = :phoneNumber AND isWhitelisted = 1)")
    suspend fun isWhitelisted(phoneNumber: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM number_list WHERE phoneNumber = :phoneNumber AND isWhitelisted = 0)")
    suspend fun isBlacklisted(phoneNumber: String): Boolean

    @Query("SELECT * FROM number_list WHERE isWhitelisted = 1 ORDER BY addedAt DESC")
    fun getWhitelistedNumbers(): Flow<List<NumberListEntity>>

    @Query("SELECT * FROM number_list WHERE isWhitelisted = 0 ORDER BY addedAt DESC")
    fun getBlacklistedNumbers(): Flow<List<NumberListEntity>>

    @Query("SELECT * FROM number_list WHERE isWhitelisted = 1")
    suspend fun getAllWhitelisted(): List<NumberListEntity>
}
