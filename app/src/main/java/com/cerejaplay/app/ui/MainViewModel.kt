package com.cerejaplay.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cerejaplay.app.data.model.AppConfig
import com.cerejaplay.app.data.model.UserSettings
import com.cerejaplay.app.data.repo.CerejaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val config: AppConfig? = null,
    val loading: Boolean = true,
    val offline: Boolean = false,
    val error: String? = null
)

class MainViewModel(private val repository: CerejaRepository) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val onboardingSeen: StateFlow<Boolean?> = repository.onboardingSeen
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setTheme(value: String) = viewModelScope.launch { repository.updateTheme(value) }
    fun setProfile(value: String) = viewModelScope.launch { repository.updateProfile(value) }
    fun completeOnboarding() = viewModelScope.launch { repository.markOnboardingSeen() }

    init {
        bootstrap()
    }

    fun bootstrap() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            if (_state.value.config == null) {
                val cached = repository.cachedConfig()
                if (cached != null) {
                    _state.update { it.copy(config = cached, loading = false) }
                }
            }
            try {
                val config = repository.loadConfig()
                _state.update { it.copy(config = config, loading = false, offline = false, error = null) }
            } catch (error: Exception) {
                if (_state.value.config != null) {
                    _state.update { it.copy(loading = false, offline = true) }
                } else {
                    _state.update {
                        it.copy(
                            config = repository.fallbackConfig(),
                            loading = false,
                            offline = true,
                            error = null
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun factory(repository: CerejaRepository) = viewModelFactory {
            initializer { MainViewModel(repository) }
        }
    }
}
