package com.colombiaguard.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.colombiaguard.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementación de ContactRepository usando ContentResolver de Android.
 * Principio SRP: Solo accede a contactos del dispositivo.
 */
class ContactRepositoryImpl(
    private val context: Context
) : ContactRepository {

    /**
     * Cache de números de contactos para evitar consultas repetidas.
     */
    private var contactNumbersCache: Set<String>? = null

    override suspend fun isNumberInContacts(phoneNumber: String): Boolean {
        val normalized = normalizeForComparison(phoneNumber)
        val contacts = getAllContactNumbers()
        return contacts.any { normalizeForComparison(it) == normalized }
    }

    override suspend fun getAllContactNumbers(): Set<String> {
        contactNumbersCache?.let { return it }

        return withContext(Dispatchers.IO) {
            val numbers = mutableSetOf<String>()

            try {
                // [REFUERZO 4] Consultar números de teléfono de contactos
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )

                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val numberColumn = cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )

                    while (cursor.moveToNext()) {
                        val number = cursor.getString(numberColumn)
                        if (!number.isNullOrBlank()) {
                            numbers.add(number)
                        }
                    }
                }
            } catch (e: SecurityException) {
                // [REFUERZO 4] Permiso READ_CONTACTS no otorgado
                // Retornar conjunto vacío en lugar de crashear
                android.util.Log.w("ContactRepo", "READ_CONTACTS permission not granted: ${e.message}")
                return@withContext emptySet()
            } catch (e: Exception) {
                android.util.Log.e("ContactRepo", "Error reading contacts: ${e.message}")
                return@withContext emptySet()
            }

            contactNumbersCache = numbers
            numbers
        }
    }

    /**
     * Normaliza un número para comparación.
     * Elimina espacios, guiones, paréntesis y el código de país.
     */
    private fun normalizeForComparison(phoneNumber: String): String {
        var num = phoneNumber.replace("\\D".toRegex(), "")

        // Si empieza con 57 (código Colombia), quitarlo
        if (num.startsWith("57") && num.length >= 12) {
            num = num.substring(2)
        }

        // Si empieza con +, quitarlo
        if (phoneNumber.startsWith("+")) {
            num = phoneNumber.substring(1).replace("\\D".toRegex(), "")
            if (num.startsWith("57") && num.length >= 12) {
                num = num.substring(2)
            }
        }

        return num
    }
}
