package com.colombiaguard.data.local

import androidx.room.*

/**
 * DAO para operaciones de llamadas.
 * Principio SRP: Solo opera sobre call_log.
 */
@Dao
interface CallLogDao {

    @Insert
    suspend fun insertCall(call: CallLogEntity): Long

    @Query("SELECT * FROM call_log WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getCallsSince(since: Long): List<CallLogEntity>

    @Query("SELECT * FROM call_log WHERE wasBlocked = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getBlockedCalls(limit: Int = 100): kotlinx.coroutines.flow.Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_log WHERE wasBlocked = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getAllowedCalls(limit: Int = 100): kotlinx.coroutines.flow.Flow<List<CallLogEntity>>

    @Query("SELECT COUNT(*) FROM call_log")
    suspend fun getTotalCalls(): Int

    @Query("SELECT COUNT(*) FROM call_log WHERE wasBlocked = 1")
    suspend fun getBlockedCount(): Int

    @Query("SELECT COUNT(*) FROM call_log WHERE wasBlocked = 0")
    suspend fun getAllowedCount(): Int

    @Query("SELECT COUNT(*) FROM call_log WHERE wasBlocked = 1 AND isColombian = 0")
    suspend fun getForeignBlockedCount(): Int

    @Query("SELECT COUNT(*) FROM call_log WHERE wasBlocked = 1 AND reason LIKE '%spam%'")
    suspend fun getSpamBlockedCount(): Int

    @Query("DELETE FROM call_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT * FROM call_log WHERE phoneNumber = :number ORDER BY timestamp DESC")
    suspend fun getCallsByNumber(number: String): List<CallLogEntity>
}
