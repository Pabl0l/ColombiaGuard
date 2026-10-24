package com.colombiaguard.domain.usecase

import com.colombiaguard.fakes.FakeNumberListRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageNumberListUseCaseTest {

	private val repository = FakeNumberListRepository()
	private val useCase = ManageNumberListUseCase(repository)

	@Test
	fun `rechaza numeros sin formato plausible`() = runTest {
		assertFalse(useCase.allowNumber("12"))
		assertFalse(useCase.blockNumber(""))
		assertTrue(useCase.observeWhitelist().first().isEmpty())
	}

	@Test
	fun `acepta el formato que escribe el usuario`() = runTest {
		assertTrue(useCase.allowNumber("+57 300 123 4567"))
		assertTrue(useCase.isAllowed("3001234567"))
	}

	@Test
	fun `asigna etiqueta descriptiva por defecto`() = runTest {
		useCase.allowNumber("3001234567")
		useCase.allowNumber("6012219872")

		val labels = useCase.observeWhitelist().first().map { it.label }
		assertTrue("labels=$labels", labels.contains("Móvil"))
		assertTrue(labels.any { it.contains("Bogotá") })
	}

	@Test
	fun `respeta la etiqueta del usuario`() = runTest {
		useCase.allowNumber("3001234567", "Mamá")

		assertEquals("Mamá", useCase.observeWhitelist().first().single().label)
	}

	@Test
	fun `bloquear y desbloquear`() = runTest {
		useCase.blockNumber("+57 300 123 4567")
		assertTrue(useCase.isBlocked("3001234567"))

		useCase.removeFromBlock("300 123 4567")
		assertFalse(useCase.isBlocked("3001234567"))
	}
}
