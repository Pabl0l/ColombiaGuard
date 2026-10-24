package com.colombiaguard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para llamadas registradas.
 */
@Entity(tableName = "call_log")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val timestamp: Long,
    val wasBlocked: Boolean,
    val reason: String,
    val isColombian: Boolean,
    val isFromContacts: Boolean = false
)
