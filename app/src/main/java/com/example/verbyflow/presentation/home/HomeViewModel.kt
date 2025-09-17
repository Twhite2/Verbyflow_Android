package com.example.verbyflow.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.verbyflow.domain.repository.UserRepository
import com.example.verbyflow.domain.repository.VoiceEmbeddingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val voiceEmbeddingRepository: VoiceEmbeddingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadUserData()
    }
    
    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        userName = user.name,
                        hasVoiceEmbedding = user.voiceEmbeddingId != null,
                        preferredLanguage = user.preferredLanguage
                    )
                    
                    // Check if the embedding actually exists
                    if (user.voiceEmbeddingId != null) {
                        val embedding = voiceEmbeddingRepository.getEmbeddingById(user.voiceEmbeddingId)
                        if (embedding == null) {
                            _uiState.value = _uiState.value.copy(
                                hasVoiceEmbedding = false
                            )
                            
                            // Update user record to reflect missing embedding
                            userRepository.updateUserVoiceEmbeddingId(user.id, null)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load user data: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class HomeUiState(
    val userName: String = "User",
    val hasVoiceEmbedding: Boolean = false,
    val preferredLanguage: String = "en",
    val error: String? = null
)
