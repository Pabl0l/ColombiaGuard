package com.colombiaguard.ui.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colombiaguard.domain.repository.ListedNumber
import com.colombiaguard.service.ColombiaGuardApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NumbersUiState(
	val numbers: List<ListedNumber> = emptyList(),
	val error: String? = null
)

/** Estado compartido por las pantallas de permitidos y bloqueados. */
class NumbersViewModel(private val app: ColombiaGuardApp) : ViewModel() {

	private val _state = MutableStateFlow(NumbersUiState())
	val state: StateFlow<NumbersUiState> = _state.asStateFlow()

	private var observeJob: Job? = null

	fun start(isWhitelist: Boolean) {
		if (observeJob?.isActive == true) return
		observeJob = viewModelScope.launch {
			val flow = if (isWhitelist) {
				app.manageNumberListUseCase.observeWhitelist()
			} else {
				app.manageNumberListUseCase.observeBlacklist()
			}
			flow.collect { numbers -> _state.update { it.copy(numbers = numbers, error = null) } }
		}
	}

	/** @param onDone recibe `true` si el número se guardó. */
	fun add(isWhitelist: Boolean, phoneNumber: String, label: String, onDone: (Boolean) -> Unit) {
		viewModelScope.launch {
			val saved = if (isWhitelist) {
				app.manageNumberListUseCase.allowNumber(phoneNumber, label)
			} else {
				app.manageNumberListUseCase.blockNumber(phoneNumber, label)
			}
			if (!saved) _state.update { it.copy(error = "Número inválido: escribe al menos 7 dígitos") }
			onDone(saved)
		}
	}

	fun remove(isWhitelist: Boolean, phoneNumber: String) {
		viewModelScope.launch {
			if (isWhitelist) {
				app.manageNumberListUseCase.removeFromAllow(phoneNumber)
			} else {
				app.manageNumberListUseCase.removeFromBlock(phoneNumber)
			}
		}
	}

	fun clearError() {
		_state.update { it.copy(error = null) }
	}
}
