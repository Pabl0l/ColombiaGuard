package com.colombiaguard.service

import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.util.Log
import com.colombiaguard.domain.entity.CallClassification
import com.colombiaguard.domain.entity.CallInfo
import com.colombiaguard.domain.entity.PhoneNumbers
import com.colombiaguard.domain.entity.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Servicio de filtrado inteligente de llamadas.
 *
 * Flujo: Android detecta la llamada → [onScreenCall] → normalización →
 * emergencias → `ScreenCallUseCase` → respuesta (ALLOW / SILENCE / BLOCK).
 *
 * Restricciones que condicionan el diseño:
 * - Android espera `respondToCall` en ~5 s: el trabajo pesado (Room, agenda)
 *   se mantiene fuera del hilo principal y se mide para detectar regresiones.
 * - Fail-safe: cualquier excepción permite la llamada. Nunca se cuelga una
 *   llamada por un bug.
 * - Una sola respuesta por llamada.
 */
class ColombiaGuardScreeningService : CallScreeningService() {

	private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	override fun onScreenCall(callDetails: Call.Details) {
		val startedAt = SystemClock.elapsedRealtime()

		// Solo llamadas entrantes.
		if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
			respondAllow(callDetails)
			return
		}

		val handle: Uri? = callDetails.handle
		val phoneNumber = PhoneNumbers.normalize(handle?.schemeSpecificPart ?: "")

		// Números vacíos/cortos o emergencias: permitir sin consultar nada.
		if (phoneNumber.length < 3 || PhoneNumbers.isEmergency(phoneNumber)) {
			respondAllow(callDetails)
			return
		}

		val callInfo = CallInfo(
			phoneNumber = phoneNumber,
			timestamp = System.currentTimeMillis(),
			// getCallerNumberVerificationStatus requiere API 30 (Android 11).
			verificationStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				when (callDetails.callerNumberVerificationStatus) {
					Connection.VERIFICATION_STATUS_PASSED -> VerificationStatus.PASSED
					Connection.VERIFICATION_STATUS_FAILED -> VerificationStatus.FAILED
					else -> VerificationStatus.NOT_VERIFIED
				}
			} else {
				VerificationStatus.NOT_VERIFIED
			}
		)

		serviceScope.launch {
			var responded = false
			try {
				val app = application as ColombiaGuardApp
				val recentCalls = app.manageCallHistoryUseCase.getRecentCalls(24)
				val result = app.screenCallUseCase.execute(callInfo, recentCalls)

				app.manageCallHistoryUseCase.logCall(callInfo, result)

				if (result.classification == CallClassification.BLOCK ||
					result.classification == CallClassification.SILENCE
				) {
					app.notifyBlockedCall(phoneNumber, result.classification, result.reason)
				}

				respondToCall(callDetails, buildResponse(result.classification))
				responded = true

				val elapsed = SystemClock.elapsedRealtime() - startedAt
				Log.d(TAG, "screened $phoneNumber -> ${result.classification} (${result.reason}) in ${elapsed}ms")
				if (elapsed > SLOW_SCREENING_MS) {
					Log.w(TAG, "screening lento: ${elapsed}ms (> ${SLOW_SCREENING_MS}ms). Revisar cache de contactos/listas.")
				}
			} catch (e: Exception) {
				Log.e(TAG, "Error screening call; permitiendo por fail-safe", e)
				if (!responded) runCatching { respondAllow(callDetails) }
			}
		}
	}

	private fun respondAllow(callDetails: Call.Details) {
		runCatching { respondToCall(callDetails, CallResponse.Builder().build()) }
	}

	/** Traduce la clasificación a la respuesta de Android. */
	private fun buildResponse(classification: CallClassification): CallResponse =
		when (classification) {
			CallClassification.BLOCK -> CallResponse.Builder()
				.setDisallowCall(true)
				.setRejectCall(true)
				.setSkipNotification(false)
				.setSkipCallLog(false)
				.build()

			CallClassification.SILENCE -> CallResponse.Builder()
				.setDisallowCall(false)
				.setRejectCall(false)
				.setSilenceCall(true)
				.setSkipNotification(false)
				.build()

			CallClassification.ALLOW, CallClassification.WHITELISTED -> CallResponse.Builder()
				.setDisallowCall(false)
				.setRejectCall(false)
				.setSilenceCall(false)
				.build()
		}

	private companion object {
		const val TAG = "ColombiaGuard"

		/** Umbral para avisar de una latencia que se acerca al límite de Android. */
		const val SLOW_SCREENING_MS = 1500L
	}
}
