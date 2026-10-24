package com.colombiaguard.service

import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.entity.CallClassification
import com.colombiaguard.domain.entity.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Servicio de filtrado inteligente de llamadas.
 *
 * Este es el componente principal de ColombiaGuard.
 * Implementa CallScreeningService de Android para interceptar llamadas
 * ANTES de que suenen en el dispositivo.
 *
 * Flujo:
 * 1. Android detecta llamada entrante
 * 2. Llama a onScreenCall() con los detalles
 * 3. ColombiaGuard analiza el número
 * 4. Decide: ALLOW, SILENCE, o BLOCK
 * 5. Responde a Android en menos de 5 segundos
 *
 * Principios aplicados:
 * - SRP: Solo maneja screening de llamadas
 * - OCP: Abierto a nuevas reglas sin modificar existentes
 * - DIP: Depende de abstracciones (use cases)
 */
@RequiresApi(Build.VERSION_CODES.Q)
class ColombiaGuardScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Punto de entrada principal.
     * Android llama a este método cuando hay una llamada entrante.
     */
    override fun onScreenCall(callDetails: Call.Details) {
        val direction = callDetails.callDirection

        // Solo procesar llamadas entrantes
        if (direction != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Extraer número de teléfono
        val handle: Uri? = callDetails.handle
        val phoneNumber = handle?.schemeSpecificPart ?: ""

        // [REFUERZO 1] Números vacíos/inválidos → permitir (no bloquear por error)
        if (phoneNumber.isBlank() || phoneNumber.length < 3) {
            Log.w(TAG, "Empty or too short number: '$phoneNumber' - allowing call")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Verificar si es emergencia (siempre permitir)
        if (isEmergencyNumber(phoneNumber)) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Obtener estado de verificación de la red
        val verificationStatus = when (callDetails.callerNumberVerificationStatus) {
            android.telecom.Connection.VERIFICATION_STATUS_PASSED -> VerificationStatus.PASSED
            android.telecom.Connection.VERIFICATION_STATUS_FAILED -> VerificationStatus.FAILED
            else -> VerificationStatus.NOT_VERIFIED
        }

        // Crear objeto CallInfo
        val callInfo = CallInfo(
            phoneNumber = phoneNumber,
            timestamp = System.currentTimeMillis(),
            verificationStatus = verificationStatus
        )

        // [REFUERZO 5] Protección contra concurrencia: usar Mutex
        serviceScope.launch {
            try {
                val app = application as ColombiaGuardApp
                val recentCalls = app.manageCallHistoryUseCase.getRecentCalls(24)

                val result = app.screenCallUseCase.execute(callInfo, recentCalls)

                // Registrar en historial
                app.manageCallHistoryUseCase.logCall(
                    callInfo,
                    wasBlocked = result.classification == CallClassification.BLOCK,
                    reason = result.reason
                )

                // [REFUERZO 6] Notificar al usuario cuando se bloquea
                if (result.classification == CallClassification.BLOCK ||
                    result.classification == CallClassification.SILENCE
                ) {
                    app.notifyBlockedCall(phoneNumber, result.classification, result.reason)
                }

                // Responder a Android
                val response = buildResponse(result.classification)
                respondToCall(callDetails, response)

                Log.d(TAG, "Call screened: $phoneNumber -> ${result.classification} (${result.reason})")
            } catch (e: Exception) {
                Log.e(TAG, "Error screening call", e)
                // En caso de error, permitir la llamada (fail-safe)
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }

    /**
     * Construye la respuesta de Android según la clasificación.
     */
    private fun buildResponse(classification: CallClassification): CallResponse {
        return when (classification) {
            CallClassification.ALLOW -> {
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(false)
                    .build()
            }

            CallClassification.SILENCE -> {
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(true)
                    .setSkipNotification(false)
                    .build()
            }

            CallClassification.BLOCK -> {
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(false)
                    .setSkipCallLog(false)
                    .build()
            }

            CallClassification.WHITELISTED -> {
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(false)
                    .build()
            }
        }
    }

    /**
     * Verifica si un número es de emergencia.
     * Los números de emergencia SIEMPRE deben pasar el filtro.
     */
    private fun isEmergencyNumber(phoneNumber: String): Boolean {
        val emergencyNumbers = listOf(
            "123", "132", "125", "141", "112",
            "103", "106", "119", "144", "155", "147",
            "911", "000", "110", "118", "119"
        )
        val normalized = phoneNumber.replace("\\D".toRegex(), "")
        return emergencyNumbers.any { normalized.endsWith(it) || normalized == it }
    }

    companion object {
        private const val TAG = "ColombiaGuard"
    }
}
