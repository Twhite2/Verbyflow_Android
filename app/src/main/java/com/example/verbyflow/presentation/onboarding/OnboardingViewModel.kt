package com.example.verbyflow.presentation.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.verbyflow.domain.model.User
import com.example.verbyflow.domain.repository.UserRepository
import com.example.verbyflow.domain.usecase.onboarding.GenerateVoiceEmbeddingUseCase
import com.example.verbyflow.ml.EcapaTdnnProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

private const val TAG = "OnboardingViewModel"

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val generateVoiceEmbeddingUseCase: GenerateVoiceEmbeddingUseCase,
    private val ecapaTdnnProcessor: EcapaTdnnProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private var recordedAudioFile: File? = null
    
    init {
        // Check if user exists and is already onboarded
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user != null) {
                _uiState.value = _uiState.value.copy(
                    isUserCreated = true,
                    userName = user.name,
                    isOnboarded = user.isOnboarded
                )
                
                if (user.voiceEmbeddingId != null) {
                    _uiState.value = _uiState.value.copy(
                        hasVoiceEmbedding = true
                    )
                }
            }
        }
    }
    
    fun updateUserName(name: String) {
        _uiState.value = _uiState.value.copy(userName = name)
    }
    
    fun createUser() {
        viewModelScope.launch {
            try {
                val userId = UUID.randomUUID().toString()
                val user = User(
                    id = userId,
                    name = _uiState.value.userName.ifEmpty { "User" }
                )
                userRepository.saveUser(user)
                
                _uiState.value = _uiState.value.copy(
                    isUserCreated = true,
                    onboardingStep = OnboardingStep.VOICE_RECORDING
                )
                
                Log.d(TAG, "User created: ${user.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating user", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to create user: ${e.message}"
                )
            }
        }
    }
    
    fun startRecording() {
        viewModelScope.launch {
            try {
                _isRecording.value = true
                _uiState.value = _uiState.value.copy(
                    recordingProgress = 0f
                )
                
                // Start a coroutine to update progress
                launch {
                    for (i in 1..100) {
                        kotlinx.coroutines.delay(50) // 5 seconds total (100 * 50ms)
                        _uiState.value = _uiState.value.copy(
                            recordingProgress = i / 100f
                        )
                        
                        if (i == 100) {
                            stopRecording()
                        }
                    }
                }
                
                // Use EcapaTdnnProcessor to record audio
                recordedAudioFile = ecapaTdnnProcessor.recordAudio(5000) // 5 seconds
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                _isRecording.value = false
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start recording: ${e.message}"
                )
            }
        }
    }
    
    fun stopRecording() {
        if (_isRecording.value) {
            _isRecording.value = false
            _uiState.value = _uiState.value.copy(
                onboardingStep = OnboardingStep.VOICE_PROCESSING,
                recordingProgress = 1f
            )
            
            processRecording()
        }
    }
    
    private fun processRecording() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isProcessing = true
                )
                
                val audioFile = recordedAudioFile ?: throw IllegalStateException("No audio recorded")
                
                // Generate embedding
                val result = generateVoiceEmbeddingUseCase(audioFile)
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        hasVoiceEmbedding = true,
                        isOnboarded = true,
                        onboardingStep = OnboardingStep.COMPLETED
                    )
                    Log.d(TAG, "Voice embedding generated successfully")
                } else {
                    throw result.exceptionOrNull() ?: IllegalStateException("Unknown error")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing recording", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Failed to process recording: ${e.message}",
                    onboardingStep = OnboardingStep.VOICE_RECORDING // Go back to recording step
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        recordedAudioFile?.delete()
    }
}

data class OnboardingUiState(
    val isUserCreated: Boolean = false,
    val userName: String = "",
    val isProcessing: Boolean = false,
    val recordingProgress: Float = 0f,
    val hasVoiceEmbedding: Boolean = false,
    val isOnboarded: Boolean = false,
    val error: String? = null,
    val onboardingStep: OnboardingStep = OnboardingStep.USER_SETUP
)

enum class OnboardingStep {
    USER_SETUP,
    VOICE_RECORDING,
    VOICE_PROCESSING,
    COMPLETED
}
