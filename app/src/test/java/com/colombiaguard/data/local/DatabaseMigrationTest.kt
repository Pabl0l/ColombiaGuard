package com.colombiaguard.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La migración 1 → 2 es el punto donde se arregla la whitelist de los usuarios
 * que ya tenían números guardados con formato libre. Se prueba contra SQLite
 * real (Robolectric), no contra un mock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val dbName = "migration-test.db"
	private lateinit var helper: SupportSQLiteOpenHelper
	private lateinit var db: SupportSQLiteDatabase

	@Before
	fun setUp() {
		context.deleteDatabase(dbName)

		val config = SupportSQLiteOpenHelper.Configuration.builder(context)
			.name(dbName)
			.callback(object : SupportSQLiteOpenHelper.Callback(1) {
				override fun onCreate(db: SupportSQLiteDatabase) {
					db.execSQL(
						"""
						CREATE TABLE IF NOT EXISTS number_list (
							phoneNumber TEXT NOT NULL PRIMARY KEY,
							label TEXT NOT NULL,
							addedAt INTEGER NOT NULL,
							isWhitelisted INTEGER NOT NULL,
							source TEXT NOT NULL
						)
						""".trimIndent()
					)
				}

				override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
			})
			.build()

		helper = FrameworkSQLiteOpenHelperFactory().create(config)
		db = helper.writableDatabase
	}

	@After
	fun tearDown() {
		helper.close()
		context.deleteDatabase(dbName)
	}

	private fun insert(number: String, label: String, whitelisted: Boolean) {
		db.execSQL(
			"INSERT OR REPLACE INTO number_list (phoneNumber, label, addedAt, isWhitelisted, source) VALUES (?, ?, ?, ?, ?)",
			arrayOf(number, label, 1L, if (whitelisted) 1 else 0, "MANUAL")
		)
	}

	private fun rows(): Map<String, Pair<String, Boolean>> {
		val result = mutableMapOf<String, Pair<String, Boolean>>()
		db.query("SELECT phoneNumber, label, isWhitelisted FROM number_list").use { cursor ->
			while (cursor.moveToNext()) {
				result[cursor.getString(0)] = cursor.getString(1) to (cursor.getInt(2) == 1)
			}
		}
		return result
	}

	@Test
	fun `normaliza los numeros guardados con formato libre`() {
		insert("+57 300 123 4567", "Mamá", whitelisted = true)
		insert("601-221-9872", "Spam fijo", whitelisted = false)
		insert("(310) 987 6543", "Trabajo", whitelisted = true)

		ColombiaGuardDatabase.MIGRATION_1_2.migrate(db)

		val result = rows()
		assertEquals(3, result.size)
		assertTrue(result.containsKey("3001234567"))
		assertTrue(result.containsKey("6012219872"))
		assertTrue(result.containsKey("3109876543"))
		assertEquals("Mamá", result.getValue("3001234567").first)
		assertEquals(false, result.getValue("6012219872").second)
	}

	@Test
	fun `colapsa duplicados que solo diferian en formato`() {
		insert("3001234567", "Uno", whitelisted = true)
		insert("+57 300 123 4567", "Dos", whitelisted = true)

		ColombiaGuardDatabase.MIGRATION_1_2.migrate(db)

		assertEquals(1, rows().size)
		assertTrue(rows().containsKey("3001234567"))
	}

	@Test
	fun `descarta filas sin digitos`() {
		insert("sin-numero", "Basura", whitelisted = true)
		insert("3001234567", "Bueno", whitelisted = true)

		ColombiaGuardDatabase.MIGRATION_1_2.migrate(db)

		val result = rows()
		assertEquals(1, result.size)
		assertTrue(result.containsKey("3001234567"))
	}
}
