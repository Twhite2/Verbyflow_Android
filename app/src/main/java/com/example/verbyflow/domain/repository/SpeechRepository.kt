package com.example.verbyflow.domain.repository

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for speech-to-text and text-to-speech operations.
 */
interface SpeechRepository {
    // Speech-to-text operations
    suspend fun transcribeAudio(audioData: ByteArray, language: String = "en"): String
    suspend fun startStreamingTranscription(language: String = "en"): Flow<String>
    suspend fun stopStreamingTranscription()
    
    // Translation operations
    suspend fun translateText(text: String, sourceLanguage: String, targetLanguage: String): String
    
    // Text-to-speech operations
    suspend fun synthesizeSpeech(text: String, voiceEmbeddingId: String?): ByteArray
    suspend fun startStreamingSynthesis(textFlow: Flow<String>, voiceEmbeddingId: String?): Flow<ByteArray>
}
