package com.colombiaguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.colombiaguard.domain.entity.PhoneNumbers

@Database(
	entities = [CallLogEntity::class, NumberListEntity::class],
	version = 2,
	exportSchema = true
)
abstract class ColombiaGuardDatabase : RoomDatabase() {

	abstract fun callLogDao(): CallLogDao
	abstract fun numberListDao(): NumberListDao

	companion object {
		@Volatile
		private var INSTANCE: ColombiaGuardDatabase? = null

		/**
		 * v1 → v2: normaliza los números ya guardados y colapsa duplicados.
		 *
		 * Sin esta migración, un número guardado como "+57 300 123 4567" nunca
		 * volvería a coincidir con el formato que entrega la red.
		 */
		val MIGRATION_1_2 = object : Migration(1, 2) {
			override fun migrate(db: SupportSQLiteDatabase) {
				data class Row(val number: String, val label: String, val addedAt: Long, val whitelisted: Int, val source: String)

				val rows = mutableListOf<Row>()
				db.query("SELECT phoneNumber, label, addedAt, isWhitelisted, source FROM number_list").use { cursor ->
					val numberIndex = cursor.getColumnIndexOrThrow("phoneNumber")
					val labelIndex = cursor.getColumnIndexOrThrow("label")
					val addedAtIndex = cursor.getColumnIndexOrThrow("addedAt")
					val whitelistedIndex = cursor.getColumnIndexOrThrow("isWhitelisted")
					val sourceIndex = cursor.getColumnIndexOrThrow("source")
					while (cursor.moveToNext()) {
						rows += Row(
							number = cursor.getString(numberIndex),
							label = cursor.getString(labelIndex),
							addedAt = cursor.getLong(addedAtIndex),
							whitelisted = cursor.getInt(whitelistedIndex),
							source = cursor.getString(sourceIndex)
						)
					}
				}

				db.execSQL("DELETE FROM number_list")
				rows.forEach { row ->
					val normalized = PhoneNumbers.normalize(row.number)
					if (normalized.isEmpty()) return@forEach
					db.execSQL(
						"INSERT OR REPLACE INTO number_list (phoneNumber, label, addedAt, isWhitelisted, source) VALUES (?, ?, ?, ?, ?)",
						arrayOf<Any>(normalized, row.label, row.addedAt, row.whitelisted, row.source)
					)
				}
			}
		}

		fun getDatabase(context: Context): ColombiaGuardDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(
					context.applicationContext,
					ColombiaGuardDatabase::class.java,
					"colombia_guard.db"
				)
					.addMigrations(MIGRATION_1_2)
					.build()
				INSTANCE = instance
				instance
			}
		}
	}
}
