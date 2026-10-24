package com.colombiaguard.domain.entity

/**
 * Representa una llamada entrante analizada por ColombiaGuard.
 * Contiene toda la información necesaria para tomar una decisión de filtrado.
 */
data class CallInfo(
    val phoneNumber: String,
    val timestamp: Long,
    val duration: Long = 0L,
    val isFromContacts: Boolean = false,
    val verificationStatus: VerificationStatus = VerificationStatus.NOT_VERIFIED
)

/**
 * Estado de verificación del número por parte de la red telefónica.
 * Proporcionado por Android CallScreeningService.
 */
enum class VerificationStatus {
    /** La red verificó que el número es válido */
    PASSED,
    /** La red determinó que el número NO es válido (posible spoofing) */
    FAILED,
    /** La red no pudo verificar el número */
    NOT_VERIFIED
}
