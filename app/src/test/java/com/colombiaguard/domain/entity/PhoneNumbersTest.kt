package com.colombiaguard.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

	@Test
	fun `normaliza formatos escritos por personas`() {
		val expected = "3001234567"
		assertEquals(expected, PhoneNumbers.normalize("+57 300 123 4567"))
		assertEquals(expected, PhoneNumbers.normalize("+573001234567"))
		assertEquals(expected, PhoneNumbers.normalize("300-123-45-67"))
		assertEquals(expected, PhoneNumbers.normalize("(300) 123 4567"))
		assertEquals(expected, PhoneNumbers.normalize("0057 3001234567"))
		assertEquals(expected, PhoneNumbers.normalize("  3001234567  "))
	}

	@Test
	fun `no recorta el prefijo de un numero extranjero`() {
		assertEquals("13051234567", PhoneNumbers.normalize("+1 305 123 4567"))
		assertEquals("34600123456", PhoneNumbers.normalize("+34 600 123 456"))
	}

	@Test
	fun `compara numeros con distinto formato`() {
		assertTrue(PhoneNumbers.sameNumber("+57 300 123 4567", "3001234567"))
		assertTrue(PhoneNumbers.sameNumber("573001234567", "+57 300 123 4567"))
		assertFalse(PhoneNumbers.sameNumber("3001234567", "3001234568"))
		assertFalse(PhoneNumbers.sameNumber("", ""))
	}

	@Test
	fun `detecta emergencias solo por igualdad exacta`() {
		assertTrue(PhoneNumbers.isEmergency("123"))
		assertTrue(PhoneNumbers.isEmergency("112"))
		assertTrue(PhoneNumbers.isEmergency(" 123 "))
		assertTrue(PhoneNumbers.isEmergency("911"))
	}

	@Test
	fun `un numero que termina en un codigo de emergencia no es emergencia`() {
		assertFalse(PhoneNumbers.isEmergency("3001234123"))
		assertFalse(PhoneNumbers.isEmergency("6012219112"))
		assertFalse(PhoneNumbers.isEmergency("3201234567"))
	}

	@Test
	fun `valida estructura E164`() {
		assertTrue(PhoneNumbers.hasValidFormat("3001234567"))
		assertTrue(PhoneNumbers.hasValidFormat("+57 601 221 9872"))
		assertFalse(PhoneNumbers.hasValidFormat("12"))
		assertFalse(PhoneNumbers.hasValidFormat(""))
	}

	@Test
	fun `formatea para mostrar`() {
		assertEquals("+57 300 123 4567", PhoneNumbers.formatDisplay("3001234567"))
		assertEquals("+57 601 221 9872", PhoneNumbers.formatDisplay("+57 601 221 9872"))
		assertEquals("sin-formato", PhoneNumbers.formatDisplay("sin-formato"))
	}
}
