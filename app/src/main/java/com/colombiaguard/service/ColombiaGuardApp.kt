package com.colombiaguard.service

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.colombiaguard.R
import com.colombiaguard.data.local.ColombiaGuardDatabase
import com.colombiaguard.data.repository.CallHistoryRepositoryImpl
import com.colombiaguard.data.repository.ContactRepositoryImpl
import com.colombiaguard.data.repository.FilterConfigRepositoryImpl
import com.colombiaguard.data.repository.NumberListRepositoryImpl
import com.colombiaguard.domain.entity.CallClassification
import com.colombiaguard.domain.entity.PhoneNumbers
import com.colombiaguard.domain.usecase.ManageCallHistoryUseCase
import com.colombiaguard.domain.usecase.ManageFilterConfigUseCase
import com.colombiaguard.domain.usecase.ManageNumberListUseCase
import com.colombiaguard.domain.usecase.ScreenCallUseCase
import com.colombiaguard.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application de ColombiaGuard: compositor de dependencias (DI manual).
 * DIP: todo se inyecta desde aquí.
 */
class ColombiaGuardApp : Application() {

	// Persistencia
	val database by lazy { ColombiaGuardDatabase.getDatabase(this) }

	// Repositorios
	val contactRepository by lazy { ContactRepositoryImpl(this) }
	val filterConfigRepository by lazy { FilterConfigRepositoryImpl(this) }
	val callHistoryRepository by lazy { CallHistoryRepositoryImpl(database.callLogDao()) }
	val numberListRepository by lazy { NumberListRepositoryImpl(database.numberListDao()) }

	// Casos de uso
	val screenCallUseCase by lazy {
		ScreenCallUseCase(contactRepository, filterConfigRepository, numberListRepository)
	}
	val manageCallHistoryUseCase by lazy { ManageCallHistoryUseCase(callHistoryRepository) }
	val manageFilterConfigUseCase by lazy { ManageFilterConfigUseCase(filterConfigRepository) }
	val manageNumberListUseCase by lazy { ManageNumberListUseCase(numberListRepository) }

	private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	private val notificationManager by lazy {
		getSystemService(NOTIFICATION_SERVICE) as NotificationManager
	}

	override fun onCreate() {
		super.onCreate()
		createNotificationChannel()

		// Precalentamiento: la primera llamada entrante no debe pagar la lectura
		// de la agenda ni la limpieza del historial dentro de los 5 s de Android.
		appScope.launch {
			runCatching { contactRepository.getAllContactNumbers() }
				.onFailure { android.util.Log.w(TAG, "No se pudo precargar contactos: ${it.message}") }
			runCatching { manageCallHistoryUseCase.purgeOldHistory() }
				.onFailure { android.util.Log.w(TAG, "No se pudo purgar el historial: ${it.message}") }
		}
	}

	private fun createNotificationChannel() {
		val channel = NotificationChannel(
			CHANNEL_ID,
			"Llamadas Bloqueadas",
			NotificationManager.IMPORTANCE_DEFAULT
		).apply {
			description = "Notificaciones cuando ColombiaGuard bloquea o silencia una llamada"
		}
		notificationManager.createNotificationChannel(channel)
	}

	/**
	 * Notifica una llamada filtrada. El icono es monocromo porque Android
	 * pinta la notificación con una máscara de un solo color.
	 */
	fun notifyBlockedCall(phoneNumber: String, classification: CallClassification, reason: String) {
		val title = when (classification) {
			CallClassification.BLOCK -> "Llamada bloqueada"
			CallClassification.SILENCE -> "Llamada silenciada"
			else -> return
		}

		val intent = Intent(this, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		}
		val pendingIntent = PendingIntent.getActivity(
			this, 0, intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_stat_colombiaguard)
			.setContentTitle(title)
			.setContentText("${PhoneNumbers.formatDisplay(phoneNumber)} - $reason")
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.build()

		notificationManager.notify(System.currentTimeMillis().toInt(), notification)
	}

	private companion object {
		const val TAG = "ColombiaGuardApp"
		const val CHANNEL_ID = "colombiaguard_blocked_calls"
	}
}
