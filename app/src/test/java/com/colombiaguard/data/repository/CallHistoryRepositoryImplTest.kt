package com.colombiaguard.data.repository

import com.colombiaguard.domain.entity.CallClassification
import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.entity.ScreeningResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallHistoryRepositoryImplTest {

	private val dao = FakeCallLogDao()
	private val repository = CallHistoryRepositoryImpl(dao)

	@Test
	fun `guarda el numero canonico y el resultado real`() = runTest {
		repository.logCall(
			CallInfo("+57 300 123 4567", timestamp = 1_000L),
			ScreeningResult(CallClassification.BLOCK, "Número en lista negra", isColombian = true)
		)

		val entity = dao.inserted.single()
		assertEquals("3001234567", entity.phoneNumber)
		assertTrue(entity.wasBlocked)
		assertTrue(entity.isColombian)
		assertEquals("Número en lista negra", entity.reason)
	}

	@Test
	fun `propaga isColombian desde el screening`() = runTest {
		repository.logCall(
			CallInfo("+1 305 123 4567", timestamp = 2_000L),
			ScreeningResult(CallClassification.BLOCK, "Llamada extranjera bloqueada", isColombian = false)
		)

		assertFalse(dao.inserted.single().isColombian)
	}

	@Test
	fun `una llamada silenciada no cuenta como bloqueada`() = runTest {
		repository.logCall(
			CallInfo("3001234567", timestamp = 3_000L),
			ScreeningResult(CallClassification.SILENCE, "Riesgo moderado", isColombian = true)
		)

		assertFalse(dao.inserted.single().wasBlocked)
	}

	@Test
	fun `las estadisticas se derivan del resultado persistido`() = runTest {
		repository.logCall(
			CallInfo("3001234567", timestamp = 1L),
			ScreeningResult(CallClassification.BLOCK, "Spam (riesgo 95%)", isColombian = true)
		)
		repository.logCall(
			CallInfo("+1 305 123 4567", timestamp = 2L),
			ScreeningResult(CallClassification.BLOCK, "Llamada extranjera bloqueada", isColombian = false)
		)
		repository.logCall(
			CallInfo("3109876543", timestamp = 3L),
			ScreeningResult(CallClassification.ALLOW, "Número colombiano válido", isColombian = true)
		)

		val stats = repository.getStats()
		assertEquals(3, stats.totalCalls)
		assertEquals(2, stats.blockedCalls)
		assertEquals(1, stats.allowedCalls)
		assertEquals(1, stats.foreignBlocked)
		assertEquals(1, stats.spamBlocked)
	}

	@Test
	fun `purga el historial segun la retencion`() = runTest {
		repository.purgeOlderThan(24)

		val before = dao.lastDeleteOlderThan
		assertNotNull(before)
		val expected = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
		assertTrue(Math.abs(before!! - expected) < 5_000L)
	}
}
