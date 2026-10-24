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
import com.colombiaguard.data.repository.*
import com.colombiaguard.domain.entity.CallClassification
import com.colombiaguard.domain.usecase.*
import com.colombiaguard.ui.MainActivity

/**
 * Application class de ColombiaGuard.
 * Funciona como compositor de dependencias (Dependency Injection manual).
 * Principio DIP: Inversion of Control - las dependencias se inyectan desde aquí.
 */
class ColombiaGuardApp : Application() {

    // Database
    val database by lazy { ColombiaGuardDatabase.getDatabase(this) }

    // Repositories
    val contactRepository by lazy { ContactRepositoryImpl(this) }
    val filterConfigRepository by lazy { FilterConfigRepositoryImpl(this) }
    val callHistoryRepository by lazy { CallHistoryRepositoryImpl(database.callLogDao()) }
    val numberListRepository by lazy { NumberListRepositoryImpl(database.numberListDao()) }

    // Use Cases
    val screenCallUseCase by lazy {
        ScreenCallUseCase(contactRepository, filterConfigRepository, numberListRepository)
    }
    val manageCallHistoryUseCase by lazy {
        ManageCallHistoryUseCase(callHistoryRepository)
    }
    val manageFilterConfigUseCase by lazy {
        ManageFilterConfigUseCase(filterConfigRepository)
    }
    val manageNumberListUseCase by lazy {
        ManageNumberListUseCase(numberListRepository)
    }

    // [REFUERZO 6] Notificaciones de llamadas bloqueadas
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Llamadas Bloqueadas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones cuando ColombiaGuard bloquea una llamada"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Envía una notificación cuando se bloquea/silencia una llamada.
     * [REFUERZO 6] El usuario sabe qué pasó con cada llamada filtrada.
     */
    fun notifyBlockedCall(phoneNumber: String, classification: CallClassification, reason: String) {
        val title = when (classification) {
            CallClassification.BLOCK -> "Llamada bloqueada"
            CallClassification.SILENCE -> "Llamada silenciada"
            else -> return
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("$phoneNumber - $reason")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "colombiaguard_blocked_calls"
    }
}
