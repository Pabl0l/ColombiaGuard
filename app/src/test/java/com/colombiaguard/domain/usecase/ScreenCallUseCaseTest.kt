package com.colombiaguard.domain.usecase

import com.colombiaguard.domain.entity.CallClassification
import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.entity.FilterConfig
import com.colombiaguard.domain.entity.FilterLevel
import com.colombiaguard.domain.entity.VerificationStatus
import com.colombiaguard.fakes.FakeCallHistoryRepository
import com.colombiaguard.fakes.FakeContactRepository
import com.colombiaguard.fakes.FakeFilterConfigRepository
import com.colombiaguard.fakes.FakeNumberListRepository
import com.colombiaguard.fakes.bogotaTimestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenCallUseCaseTest {

	private val mobile = "3001234567"

	private fun useCase(
		contacts: Set<String> = emptySet(),
		contactsFail: Boolean = false,
		config: FilterConfig = FilterConfig(),
		whitelist: Set<String> = emptySet(),
		blacklist: Set<String> = emptySet()
	) = ScreenCallUseCase(
		FakeContactRepository(contacts, contactsFail),
		FakeFilterConfigRepository(config),
		FakeNumberListRepository(whitelist, blacklist)
	)

	private fun call(
		number: String = mobile,
		hour: Int = 12,
		verification: VerificationStatus = VerificationStatus.NOT_VERIFIED
	) = CallInfo(number, bogotaTimestamp(hour), verificationStatus = verification)

	@Test
	fun `las emergencias siempre pasan aunque esten en la lista negra`() = runTest {
		val result = useCase(blacklist = setOf("123")).execute(call("123"))
		assertEquals(CallClassification.ALLOW, result.classification)
		assertEquals("Número de emergencia", result.reason)
	}

	@Test
	fun `un numero que termina en 123 no se trata como emergencia`() = runTest {
		val result = useCase().execute(call("3001234123"))
		assertEquals(CallClassification.ALLOW, result.classification)
		assertTrue(result.reason.contains("válido"))
	}

	@Test
	fun `la lista negra bloquea comparando formatos distintos`() = runTest {
		val result = useCase(blacklist = setOf("3001234567"))
			.execute(call("+57 300 123 4567"))
		assertEquals(CallClassification.BLOCK, result.classification)
		assertEquals("Número en lista negra", result.reason)
	}

	@Test
	fun `la lista blanca permite incluso en modo estricto`() = runTest {
		val result = useCase(
			config = FilterConfig(level = FilterLevel.STRICT),
			whitelist = setOf("+57 300 123 4567")
		).execute(call())
		assertEquals(CallClassification.WHITELISTED, result.classification)
	}

	@Test
	fun `un contacto pasa en modo normal`() = runTest {
		val result = useCase(contacts = setOf(mobile)).execute(call())
		assertEquals(CallClassification.ALLOW, result.classification)
		assertEquals("Contacto del usuario", result.reason)
	}

	@Test
	fun `si falla el permiso de contactos no se bloquea la llamada`() = runTest {
		val result = useCase(contactsFail = true).execute(call())
		assertEquals(CallClassification.ALLOW, result.classification)
	}

	@Test
	fun `extranjero se bloquea en modo normal`() = runTest {
		val result = useCase().execute(call("+1 305 123 4567"))
		assertEquals(CallClassification.BLOCK, result.classification)
		assertFalse(result.isColombian)
	}

	@Test
	fun `extranjero se silencia si el bloqueo esta desactivado`() = runTest {
		val result = useCase(config = FilterConfig(blockForeignCalls = false))
			.execute(call("+1 305 123 4567"))
		assertEquals(CallClassification.SILENCE, result.classification)
	}

	@Test
	fun `una verificacion fallida bloquea en modo normal`() = runTest {
		val result = useCase().execute(call(verification = VerificationStatus.FAILED))
		assertEquals(CallClassification.BLOCK, result.classification)
		assertEquals("Verificación de número falló", result.reason)
	}

	@Test
	fun `modo estricto bloquea extranjeros`() = runTest {
		val result = useCase(config = FilterConfig(level = FilterLevel.STRICT))
			.execute(call("+34 600 123 456"))
		assertEquals(CallClassification.BLOCK, result.classification)
	}

	@Test
	fun `modo estricto bloquea numeros no reconocidos`() = runTest {
		val result = useCase(config = FilterConfig(level = FilterLevel.STRICT)).execute(call("5551234"))
		assertEquals(CallClassification.BLOCK, result.classification)
		assertEquals("Número no reconocido como colombiano", result.reason)
	}

	@Test
	fun `modo estricto permite moviles colombianos`() = runTest {
		val result = useCase(config = FilterConfig(level = FilterLevel.STRICT)).execute(call())
		assertEquals(CallClassification.ALLOW, result.classification)
	}

	@Test
	fun `modo permisivo bloquea spam de alta confianza`() = runTest {
		val result = useCase(config = FilterConfig(level = FilterLevel.PERMISSIVE))
			.execute(call(verification = VerificationStatus.FAILED))
		assertEquals(CallClassification.BLOCK, result.classification)
	}

	@Test
	fun `el patron de llamadas repetidas bloquea en modo normal`() = runTest {
		val repeated = (1..4).map { CallInfo(mobile, bogotaTimestamp(11, 50 - it * 5)) }
		val result = useCase().execute(call(), repeated)
		assertEquals(CallClassification.BLOCK, result.classification)
		assertTrue(result.isKnownSpamPattern)
	}

	@Test
	fun `la deteccion de patrones desactivada no escala el riesgo`() = runTest {
		val repeated = (1..4).map { CallInfo(mobile, bogotaTimestamp(11, 50 - it * 5)) }
		val result = useCase(config = FilterConfig(enablePatternDetection = false))
			.execute(call(), repeated)
		assertEquals(CallClassification.ALLOW, result.classification)
	}

	@Test
	fun `un numero vacio se permite por seguridad`() = runTest {
		val result = useCase().execute(call(""))
		assertEquals(CallClassification.ALLOW, result.classification)
	}
}
