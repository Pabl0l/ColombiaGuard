package com.colombiaguard.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regresión del fallo más grave del proyecto: si el número guardado no se
 * normaliza, la whitelist deja de coincidir con lo que entrega la red.
 */
class NumberListRepositoryImplTest {

	private val dao = FakeNumberListDao()
	private val repository = NumberListRepositoryImpl(dao)

	@Test
	fun `la whitelist coincide sin importar el formato`() = runTest {
		repository.addToWhitelist("+57 300 123-4567", "Mamá")

		assertTrue(repository.isWhitelisted("3001234567"))
		assertTrue(repository.isWhitelisted("+573001234567"))
		assertTrue(repository.isWhitelisted("+57 300 123 4567"))
	}

	@Test
	fun `la blacklist coincide sin importar el formato`() = runTest {
		repository.addToBlacklist("(601) 221 9872", "Spam fijo")

		assertTrue(repository.isBlacklisted("6012219872"))
		assertTrue(repository.isBlacklisted("+57 601 221 9872"))
	}

	@Test
	fun `un numero no esta en la lista contraria`() = runTest {
		repository.addToWhitelist("3001234567", "Permitido")

		assertFalse(repository.isBlacklisted("3001234567"))
	}

	@Test
	fun `eliminar funciona con otro formato`() = runTest {
		repository.addToWhitelist("3001234567", "Permitido")
		repository.removeFromWhitelist("+57 300 123 4567")

		assertFalse(repository.isWhitelisted("3001234567"))
	}

	@Test
	fun `la lista expone el numero canonico`() = runTest {
		repository.addToWhitelist("+57 300 123 4567", "Mamá")
		val items = repository.getWhitelistedNumbers().first()

		assertEquals(1, items.size)
		assertEquals("3001234567", items.single().phoneNumber)
		assertEquals("Mamá", items.single().label)
	}

	@Test
	fun `agregar el mismo numero dos veces no duplica`() = runTest {
		repository.addToWhitelist("3001234567", "A")
		repository.addToWhitelist("+57 300 123 4567", "B")

		assertEquals(1, repository.getWhitelistedNumbers().first().size)
	}
}
