package com.example.verbyflow.presentation.call

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.verbyflow.domain.model.CallStatus
import com.example.verbyflow.domain.repository.CallRepository
import com.example.verbyflow.domain.repository.SpeechRepository
import com.example.verbyflow.domain.repository.UserRepository
import com.example.verbyflow.domain.repository.VoiceEmbeddingRepository
import com.example.verbyflow.domain.usecase.call.ProcessSpeechUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CallViewModel"

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val speechRepository: SpeechRepository,
    private val userRepository: UserRepository,
    private val voiceEmbeddingRepository: VoiceEmbeddingRepository,
    private val processSpeechUseCase: ProcessSpeechUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()
    
    private var processSpeechJob: kotlinx.coroutines.Job? = null
    
    init {
        // Observe call state
        viewModelScope.launch {
            callRepository.observeCallState().collect { callSession ->
                _uiState.value = _uiState.value.copy(
                    callStatus = when(callSession?.status) {
                        CallStatus.INITIATING -> "Initiating..."
                        CallStatus.CONNECTING -> "Connecting..."
                        CallStatus.CONNECTED -> "Connected"
                        CallStatus.DISCONNECTED -> "Disconnected"
                        CallStatus.FAILED -> "Call Failed"
                        null -> "Ready"
                    },
                    isInCall = callSession?.status == CallStatus.CONNECTED || 
                               callSession?.status == CallStatus.CONNECTING
                )
            }
        }
        
        // Initialize WebRTC
        viewModelScope.launch {
            try {
                callRepository.initializeWebRTC()
                Log.d(TAG, "WebRTC initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing WebRTC", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to initialize call system: ${e.message}"
                )
            }
        }
    }
    
    fun startCall() {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser() ?: throw IllegalStateException("No user found")
                
                // Create a call offer (in a real app, we would connect to another user)
                callRepository.createCallOffer(currentUser)
                
                // Start audio capture
                callRepository.startAudioCapture()
                
                _uiState.value = _uiState.value.copy(
                    isInCall = true,
                    isMuted = false
                )
                
                // Start processing speech
                startSpeechProcessing()
                
                Log.d(TAG, "Call started")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting call", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start call: ${e.message}"
                )
            }
        }
    }
    
    fun endCall() {
        viewModelScope.launch {
            try {
                // Stop processing speech
                stopSpeechProcessing()
                
                // Stop audio capture
                callRepository.stopAudioCapture()
                
                // End the call session
                val callSession = callRepository.observeCallState().first()
                if (callSession != null) {
                    callRepository.endCall(callSession)
                }
                
                _uiState.value = _uiState.value.copy(
                    isInCall = false,
                    isMuted = false,
                    callStatus = "Ready"
                )
                
                Log.d(TAG, "Call ended")
            } catch (e: Exception) {
                Log.e(TAG, "Error ending call", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to end call: ${e.message}"
                )
            }
        }
    }
    
    fun toggleMute() {
        _uiState.value = _uiState.value.copy(
            isMuted = !_uiState.value.isMuted
        )
        
        // In a real implementation, we would mute the audio track
        // For now, we'll just update the UI state
    }
    
    private fun startSpeechProcessing() {
        processSpeechJob = viewModelScope.launch {
            try {
                val targetLanguage = "fr" // Default target language (would be configurable in a real app)
                
                processSpeechUseCase(targetLanguage).collect { result ->
                    when (result) {
                        is ProcessSpeechUseCase.ProcessingResult.Transcription -> {
                            _uiState.value = _uiState.value.copy(
                                originalTranscription = result.text
                            )
                        }
                        is ProcessSpeechUseCase.ProcessingResult.Translation -> {
                            _uiState.value = _uiState.value.copy(
                                translatedText = result.text
                            )
                        }
                        is ProcessSpeechUseCase.ProcessingResult.SynthesizedSpeech -> {
                            // In a real app, we would play this audio or send it over WebRTC
                            Log.d(TAG, "Received synthesized speech: ${result.audioData.size} bytes")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in speech processing", e)
                _uiState.value = _uiState.value.copy(
                    error = "Speech processing error: ${e.message}"
                )
            }
        }
    }
    
    private fun stopSpeechProcessing() {
        processSpeechJob?.cancel()
        processSpeechJob = null
        
        viewModelScope.launch {
            try {
                speechRepository.stopStreamingTranscription()
                _uiState.value = _uiState.value.copy(
                    originalTranscription = "",
                    translatedText = ""
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech processing", e)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            stopSpeechProcessing()
            val callSession = callRepository.observeCallState().first()
            if (callSession != null) {
                callRepository.endCall(callSession)
            }
        }
    }
}

data class CallUiState(
    val isInCall: Boolean = false,
    val isMuted: Boolean = false,
    val callStatus: String = "Ready",
    val originalTranscription: String = "",
    val translatedText: String = "",
    val error: String? = null
)
