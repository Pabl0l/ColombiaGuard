package com.colombiaguard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para números en whitelist/blacklist.
 */
@Entity(tableName = "number_list")
data class NumberListEntity(
    @PrimaryKey val phoneNumber: String,
    val label: String,
    val addedAt: Long,
    val isWhitelisted: Boolean, // true = whitelist, false = blacklist
    val source: String // "MANUAL", "CONTACTS", "SYSTEM"
)
