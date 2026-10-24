package com.colombiaguard.data.repository

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import com.colombiaguard.domain.entity.PhoneNumbers
import com.colombiaguard.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementación de ContactRepository usando ContentResolver.
 *
 * Optimización para la ventana de 5 s del CallScreeningService:
 * - la agenda se lee una sola vez y se cachea **ya normalizada**;
 * - `isNumberInContacts` pasa a ser una búsqueda O(1) en un `Set`;
 * - el cache se invalida con un [ContentObserver] cuando cambia la agenda,
 *   en lugar de quedar obsoleto hasta reiniciar el proceso.
 */
class ContactRepositoryImpl(
	private val context: Context
) : ContactRepository {

	@Volatile
	private var cache: Set<String>? = null

	@Volatile
	private var lastTenCache: Set<String> = emptySet()

	private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
		override fun onChange(selfChange: Boolean) {
			Log.d(TAG, "Contacts changed, invalidating cache")
			invalidate()
		}
	}

	init {
		runCatching {
			context.contentResolver.registerContentObserver(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
				true,
				observer
			)
		}
	}

	override suspend fun isNumberInContacts(phoneNumber: String): Boolean {
		val normalized = PhoneNumbers.normalize(phoneNumber)
		if (normalized.isEmpty()) return false
		getAllContactNumbers()
		if (cache?.contains(normalized) == true) return true
		// Tolerancia a contactos guardados sin indicativo (7 dígitos).
		return normalized.length >= 10 && lastTenCache.contains(normalized.takeLast(10))
	}

	override suspend fun getAllContactNumbers(): Set<String> {
		cache?.let { return it }

		return withContext(Dispatchers.IO) {
			val numbers = mutableSetOf<String>()
			val lastTen = mutableSetOf<String>()

			try {
				val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
				context.contentResolver.query(
					ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
					projection,
					null,
					null,
					null
				)?.use { cursor ->
					val index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
					while (cursor.moveToNext()) {
						val normalized = PhoneNumbers.normalize(cursor.getString(index) ?: continue)
						if (normalized.isNotEmpty()) {
							numbers.add(normalized)
							if (normalized.length >= 10) lastTen.add(normalized.takeLast(10))
						}
					}
				}
			} catch (e: SecurityException) {
				Log.w(TAG, "READ_CONTACTS no concedido: ${e.message}")
				return@withContext emptySet()
			} catch (e: Exception) {
				Log.e(TAG, "Error leyendo contactos: ${e.message}")
				return@withContext emptySet()
			}

			lastTenCache = lastTen
			cache = numbers
			numbers
		}
	}

	/** Fuerza la relectura de la agenda en la próxima consulta. */
	fun invalidate() {
		cache = null
	}

	private companion object {
		const val TAG = "ContactRepo"
	}
}
