package com.example.verbyflow.presentation.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.verbyflow.domain.model.User
import com.example.verbyflow.domain.repository.UserRepository
import com.example.verbyflow.domain.repository.VoiceEmbeddingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ProfileSettingsViewModel"

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val voiceEmbeddingRepository: VoiceEmbeddingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSettingsUiState())
    val uiState: StateFlow<ProfileSettingsUiState> = _uiState.asStateFlow()
    
    // Available languages for translation
    val availableLanguages = listOf(
        Language("en", "English"),
        Language("fr", "French"),
        Language("es", "Spanish"),
        Language("de", "German"),
        Language("it", "Italian"),
        Language("pt", "Portuguese"),
        Language("ru", "Russian"),
        Language("zh", "Chinese"),
        Language("ja", "Japanese"),
        Language("ko", "Korean"),
        Language("ar", "Arabic"),
        Language("hi", "Hindi")
    )
    
    init {
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        userName = user.name,
                        selectedLanguageCode = user.preferredLanguage,
                        hasVoiceEmbedding = user.voiceEmbeddingId != null,
                        isLoading = false
                    )
                    
                    // Check if voice embedding exists
                    user.voiceEmbeddingId?.let { embeddingId ->
                        val embedding = voiceEmbeddingRepository.getEmbeddingById(embeddingId)
                        if (embedding == null) {
                            _uiState.value = _uiState.value.copy(hasVoiceEmbedding = false)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load profile: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun updateUserName(name: String) {
        _uiState.value = _uiState.value.copy(userName = name)
    }
    
    fun updateSelectedLanguage(languageCode: String) {
        _uiState.value = _uiState.value.copy(selectedLanguageCode = languageCode)
    }
    
    fun saveProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                
                val currentUser = userRepository.getCurrentUser()
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        name = _uiState.value.userName,
                        preferredLanguage = _uiState.value.selectedLanguageCode
                    )
                    userRepository.saveUser(updatedUser)
                    
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                    
                    // Reset success flag after showing it briefly
                    kotlinx.coroutines.delay(2000)
                    _uiState.value = _uiState.value.copy(saveSuccess = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user profile", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save profile: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class Language(
    val code: String,
    val name: String
)

data class ProfileSettingsUiState(
    val userName: String = "",
    val selectedLanguageCode: String = "en",
    val hasVoiceEmbedding: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)
