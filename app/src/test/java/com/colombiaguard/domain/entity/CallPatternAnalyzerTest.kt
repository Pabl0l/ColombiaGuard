package com.colombiaguard.domain.entity

import com.colombiaguard.fakes.bogotaTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallPatternAnalyzerTest {

	private val number = "3001234567"

	private fun call(
		number: String = this.number,
		hour: Int = 12,
		duration: Long = 30_000
	) = CallInfo(number, bogotaTimestamp(hour), duration = duration)

	@Test
	fun `una llamada normal no genera indicadores`() {
		val indicators = CallPatternAnalyzer.analyzeCall(call(), emptyList())
		assertTrue("esperaba sin indicadores, fue $indicators", indicators.isEmpty())
	}

	@Test
	fun `una llamada colgada rapido se marca`() {
		val indicators = CallPatternAnalyzer.analyzeCall(call(duration = 2_000), emptyList())
		assertEquals(1, indicators.size)
		assertEquals(CallPatternAnalyzer.SpamType.QUICK_HANGUP, indicators.single().type)
	}

	@Test
	fun `cuatro llamadas del mismo numero en 24h se consideran spam`() {
		val recent = (1..4).map { call(hour = 11 - it) }
		val indicators = CallPatternAnalyzer.analyzeCall(call(), recent)

		assertTrue(indicators.any { it.type == CallPatternAnalyzer.SpamType.REPEATED_CALLS })
		assertTrue(CallPatternAnalyzer.calculateRiskScore(indicators) >= 0.9f)
	}

	@Test
	fun `las llamadas viejas no cuentan como repetidas`() {
		val recent = (1..4).map { call(hour = 12, duration = 30_000) }.map {
			it.copy(timestamp = it.timestamp - 48 * 60 * 60 * 1000L)
		}
		val indicators = CallPatternAnalyzer.analyzeCall(call(), recent)
		assertFalse(indicators.any { it.type == CallPatternAnalyzer.SpamType.REPEATED_CALLS })
	}

	@Test
	fun `compara numeros con distinto formato en el historial`() {
		val recent = listOf(
			CallInfo("+57 300 123 4567", bogotaTimestamp(11, 50)),
			CallInfo("+573001234567", bogotaTimestamp(11, 45)),
			CallInfo("3001234567", bogotaTimestamp(11, 40))
		)
		val indicators = CallPatternAnalyzer.analyzeCall(call(), recent)
		assertTrue(indicators.any { it.type == CallPatternAnalyzer.SpamType.REPEATED_CALLS })
	}

	@Test
	fun `una verificacion fallida es evidencia fuerte`() {
		val indicators = CallPatternAnalyzer.analyzeCall(
			call().copy(verificationStatus = VerificationStatus.FAILED),
			emptyList()
		)
		assertTrue(indicators.any { it.type == CallPatternAnalyzer.SpamType.FAILED_VERIFICATION })
	}

	@Test
	fun `detecta patrones de numeracion sospechosos`() {
		listOf("1111111111", "1234567", "7654321").forEach { suspicious ->
			val indicators = CallPatternAnalyzer.analyzeCall(call(suspicious), emptyList())
			assertTrue(
				"$suspicious debería ser sospechoso",
				indicators.any { it.type == CallPatternAnalyzer.SpamType.SUSPICIOUS_PATTERN }
			)
		}
	}

	@Test
	fun `un movil comun no es patron sospechoso`() {
		assertFalse(
			CallPatternAnalyzer.analyzeCall(call("3001234567"), emptyList())
				.any { it.type == CallPatternAnalyzer.SpamType.SUSPICIOUS_PATTERN }
		)
	}

	@Test
	fun `el score sin indicadores es cero`() {
		assertEquals(0f, CallPatternAnalyzer.calculateRiskScore(emptyList()), 0.0001f)
	}

	@Test
	fun `horario nocturno en hora de Colombia`() {
		assertTrue(CallPatternAnalyzer.isOffHours(bogotaTimestamp(23)))
		assertTrue(CallPatternAnalyzer.isOffHours(bogotaTimestamp(5)))
		assertFalse(CallPatternAnalyzer.isOffHours(bogotaTimestamp(9)))
		assertFalse(CallPatternAnalyzer.isOffHours(bogotaTimestamp(20)))
	}
}
