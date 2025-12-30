package io.hellosaumil.pocketorrery

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SolarSystemUiState(
    val selectedPlanet: Planet? = null,
    val isPaused: Boolean = false,
    val scale: Float = 1.0f
)

class SolarSystemViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SolarSystemUiState())
    val uiState: StateFlow<SolarSystemUiState> = _uiState.asStateFlow()

    fun selectPlanet(planet: Planet?) {
        // Toggle: if already selected, deselect it
        val newSelection = if (_uiState.value.selectedPlanet == planet) null else planet
        _uiState.update { it.copy(selectedPlanet = newSelection) }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }
    
    fun setScale(scale: Float) {
        _uiState.update { it.copy(scale = scale) }
    }
}
