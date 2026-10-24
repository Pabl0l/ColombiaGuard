package com.colombiaguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver que se ejecuta cuando el dispositivo se enciende.
 * Asegura que el servicio de filtrado esté activo después de un reinicio.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("ColombiaGuard", "Device boot completed, CallScreeningService will auto-activate")
            // El CallScreeningService se activa automáticamente por Android
            // cuando el usuario lo ha seleccionado como servicio de screening.
            // No necesitamos hacer nada explícito aquí.
        }
    }
}
