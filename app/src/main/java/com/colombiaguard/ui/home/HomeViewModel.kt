package com.colombiaguard.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colombiaguard.domain.entity.FilterConfig
import com.colombiaguard.domain.entity.FilterLevel
import com.colombiaguard.domain.repository.CallStats
import com.colombiaguard.service.ColombiaGuardApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
	val stats: CallStats? = null,
	val config: FilterConfig = FilterConfig(),
	val isLoading: Boolean = true
)

/**
 * Estado del dashboard. La configuración se observa como Flow, así que un
 * cambio de nivel se refleja sin recargar la pantalla.
 */
class HomeViewModel(private val app: ColombiaGuardApp) : ViewModel() {

	private val _state = MutableStateFlow(HomeUiState())
	val state: StateFlow<HomeUiState> = _state.asStateFlow()

	init {
		viewModelScope.launch {
			app.manageFilterConfigUseCase.observeConfig().collect { config ->
				_state.update { it.copy(config = config, isLoading = false) }
			}
		}
		refresh()
	}

	/** Recalcula las estadísticas (al entrar a la pantalla o volver a ella). */
	fun refresh() {
		viewModelScope.launch {
			val stats = runCatching { app.manageCallHistoryUseCase.getStats() }.getOrNull()
			_state.update { it.copy(stats = stats, isLoading = false) }
		}
	}

	fun setLevel(level: FilterLevel) {
		viewModelScope.launch { app.manageFilterConfigUseCase.setFilterLevel(level) }
	}
}
