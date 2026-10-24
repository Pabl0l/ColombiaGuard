package com.colombiaguard.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.colombiaguard.service.ColombiaGuardApp

/**
 * Crea un ViewModel inyectándole el grafo de dependencias de la Application
 * (DI manual, sin frameworks).
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
	crossinline create: (ColombiaGuardApp) -> VM
): VM {
	val app = LocalContext.current.applicationContext as ColombiaGuardApp
	return viewModel { create(app) }
}
