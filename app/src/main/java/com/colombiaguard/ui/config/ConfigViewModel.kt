package com.colombiaguard.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colombiaguard.domain.entity.FilterConfig
import com.colombiaguard.service.ColombiaGuardApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConfigViewModel(private val app: ColombiaGuardApp) : ViewModel() {

	private val _config = MutableStateFlow(FilterConfig())
	val config: StateFlow<FilterConfig> = _config.asStateFlow()

	init {
		viewModelScope.launch {
			app.manageFilterConfigUseCase.observeConfig().collect { _config.value = it }
		}
	}

	fun setBlockForeign(enabled: Boolean) = viewModelScope.launch {
		app.manageFilterConfigUseCase.setBlockForeign(enabled)
	}

	fun setSilenceUnknown(enabled: Boolean) = viewModelScope.launch {
		app.manageFilterConfigUseCase.setSilenceUnknown(enabled)
	}

	fun setPatternDetection(enabled: Boolean) = viewModelScope.launch {
		app.manageFilterConfigUseCase.setPatternDetection(enabled)
	}

	fun setWhitelistContacts(enabled: Boolean) = viewModelScope.launch {
		app.manageFilterConfigUseCase.setWhitelistContacts(enabled)
	}

	fun reset() = viewModelScope.launch {
		app.manageFilterConfigUseCase.resetToDefaults()
	}
}
