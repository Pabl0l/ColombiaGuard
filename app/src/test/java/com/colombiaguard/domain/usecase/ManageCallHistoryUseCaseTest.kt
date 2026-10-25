package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.CallClassification
import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.entity.ScreeningResult
import com.colombiaguard.domain.repository.CallHistoryRepository
import com.colombiaguard.fakes.FakeCallHistoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ManageCallHistoryUseCaseTest {

	private val call = CallInfo("3001234567", timestamp = 1_000L)
	private val result =
		ScreeningResult(CallClassification.BLOCK, "Spam (riesgo 95%)", isColombian = true)

	@Test
	fun `registra la llamada con el resultado del screening`() = runTest {
		val repository = FakeCallHistoryRepository()
		val useCase = ManageCallHistoryUseCase(repository)

		useCase.logCall(call, result)

		assertEquals(1, repository.logged.size)
		assertEquals(call, repository.logged.single().first)
		assertSame(result, repository.logged.single().second)
	}

	@Test
	fun `las llamadas recientes usan 24 horas por defecto`() = runTest {
		val repository = FakeCallHistoryRepository(recent = listOf(call))
		val useCase = ManageCallHistoryUseCase(repository)

		val recent = useCase.getRecentCalls()

		assertEquals(listOf(call), recent)
		assertEquals(24, repository.lastRecentHours)
	}

	@Test
	fun `las llamadas recientes respetan la ventana pedida`() = runTest {
		val repository = FakeCallHistoryRepository()
		val useCase = ManageCallHistoryUseCase(repository)

		useCase.getRecentCalls(hoursBack = 72)

		assertEquals(72, repository.lastRecentHours)
	}

	@Test
	fun `expone las estadisticas del repositorio`() = runTest {
		val useCase = ManageCallHistoryUseCase(FakeCallHistoryRepository())

		val stats = useCase.getStats()

		assertEquals(0, stats.totalCalls)
		assertEquals(0, stats.blockedCalls)
	}

	@Test
	fun `purga con la retencion de 30 dias por defecto`() = runTest {
		val repository = FakeCallHistoryRepository()
		val useCase = ManageCallHistoryUseCase(repository)

		useCase.purgeOldHistory()

		assertEquals(CallHistoryRepository.DEFAULT_RETENTION_HOURS, repository.lastPurgeHours)
		assertEquals(24 * 30, repository.lastPurgeHours)
	}

	@Test
	fun `purga respetando una retencion personalizada`() = runTest {
		val repository = FakeCallHistoryRepository()
		val useCase = ManageCallHistoryUseCase(repository)

		useCase.purgeOldHistory(hoursBack = 48)

		assertEquals(48, repository.lastPurgeHours)
	}
}
