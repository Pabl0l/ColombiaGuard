package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.FilterLevel
import com.colombiaguard.fakes.FakeFilterConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageFilterConfigUseCaseTest {

	private val repository = FakeFilterConfigRepository()
	private val useCase = ManageFilterConfigUseCase(repository)

	@Test
	fun `cambia el nivel de filtro`() = runTest {
		useCase.setFilterLevel(FilterLevel.STRICT)

		assertEquals(FilterLevel.STRICT, useCase.getCurrentConfig().level)
		assertEquals(FilterLevel.STRICT, useCase.observeConfig().first().level)
	}

	@Test
	fun `alterna cada opcion sin perder las demas`() = runTest {
		useCase.setBlockForeign(false)
		useCase.setSilenceUnknown(true)
		useCase.setPatternDetection(false)
		useCase.setWhitelistContacts(false)

		val config = useCase.getCurrentConfig()
		assertFalse(config.blockForeignCalls)
		assertTrue(config.silenceUnknownColombian)
		assertFalse(config.enablePatternDetection)
		assertFalse(config.whitelistAllContacts)
	}

	@Test
	fun `restablece los valores por defecto`() = runTest {
		useCase.setFilterLevel(FilterLevel.PERMISSIVE)
		useCase.setBlockForeign(false)

		useCase.resetToDefaults()

		val config = useCase.getCurrentConfig()
		assertEquals(FilterLevel.NORMAL, config.level)
		assertTrue(config.blockForeignCalls)
	}

	@Test
	fun `propaga los cambios al flujo observable`() = runTest {
		useCase.setSilenceUnknown(true)

		assertTrue(useCase.observeConfig().first().silenceUnknownColombian)
	}
}
