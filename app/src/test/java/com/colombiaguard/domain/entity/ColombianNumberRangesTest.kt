package com.colombiaguard.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColombianNumberRangesTest {

	@Test
	fun `reconoce moviles por estructura sin depender del prefijo publicado`() {
		listOf("3001234567", "3109876543", "3201112233", "3507778899", "+57 301 555 6677").forEach { raw ->
			val info = ColombianNumberRanges.analyzeNumber(raw)
			assertTrue("$raw debería ser móvil", info.isMobile)
			assertTrue("$raw debería ser colombiano", info.isColombian)
			assertEquals("Móvil", info.label)
		}
	}

	@Test
	fun `reconoce fijos con codigo de area vigente`() {
		val bogota = ColombianNumberRanges.analyzeNumber("+57 601 221 9872")
		assertTrue(bogota.isFixed)
		assertTrue(bogota.isColombian)
		assertTrue(bogota.label.contains("Bogotá"))

		val medellin = ColombianNumberRanges.analyzeNumber("6044445566")
		assertTrue(medellin.isFixed)
		assertTrue(medellin.label.contains("Medellín"))
	}

	@Test
	fun `un codigo de area inexistente no se marca como colombiano`() {
		val info = ColombianNumberRanges.analyzeNumber("6032219872")
		assertFalse(info.isColombian)
		assertFalse(info.isMobile)
		assertFalse(info.isFixed)
	}

	@Test
	fun `detecta numeros extranjeros`() {
		val usa = ColombianNumberRanges.analyzeNumber("+1 305 123 4567")
		assertTrue(usa.isForeign)
		assertFalse(usa.isColombian)
		assertTrue(usa.label.contains("USA"))

		val españa = ColombianNumberRanges.analyzeNumber("+34 600 123 456")
		assertTrue(españa.isForeign)
	}

	@Test
	fun `las emergencias son especiales y colombianas`() {
		val info = ColombianNumberRanges.analyzeNumber("123")
		assertTrue(info.isSpecial)
		assertTrue(info.isColombian)
		assertFalse(info.isForeign)
	}

	@Test
	fun `numeros cortos o basura no rompen el analisis`() {
		val info = ColombianNumberRanges.analyzeNumber("4567")
		assertFalse(info.isColombian)
		assertFalse(info.isForeign)
		assertFalse(info.isValid)
		assertEquals("Desconocido", info.label)

		assertEquals("Desconocido", ColombianNumberRanges.analyzeNumber("").label)
	}

	@Test
	fun `expone el numero canonico`() {
		assertEquals("3001234567", ColombianNumberRanges.analyzeNumber("+57 300 123 4567").normalizedNumber)
	}
}
