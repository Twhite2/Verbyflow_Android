package com.example.verbyflow.data.repository

import android.content.Context
import android.util.Log
import com.example.verbyflow.domain.repository.SpeechRepository
import com.example.verbyflow.domain.repository.VoiceEmbeddingRepository
import com.example.verbyflow.ml.WhisperProcessor
import com.example.verbyflow.ml.TranslationProcessor
import com.example.verbyflow.ml.TTSProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SpeechRepositoryImpl"

@Singleton
class SpeechRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whisperProcessor: WhisperProcessor,
    private val translationProcessor: TranslationProcessor,
    private val ttsProcessor: TTSProcessor,
    private val voiceEmbeddingRepository: VoiceEmbeddingRepository
) : SpeechRepository {

    private var isStreaming = false

    override suspend fun transcribeAudio(audioData: ByteArray, language: String): String = withContext(Dispatchers.IO) {
        try {
            whisperProcessor.transcribe(audioData, language)
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing audio", e)
            ""
        }
    }

    override suspend fun startStreamingTranscription(language: String): Flow<String> = callbackFlow {
        if (isStreaming) {
            close()
            return@callbackFlow
        }
        
        isStreaming = true
        
        val listener = object : WhisperProcessor.TranscriptionListener {
            override fun onTranscription(text: String) {
                trySend(text)
            }
            
            override fun onError(error: String) {
                Log.e(TAG, "Transcription error: $error")
                close()
            }
        }
        
        whisperProcessor.startStreamingTranscription(language, listener)
        
        awaitClose {
            isStreaming = false
            whisperProcessor.stopStreamingTranscription()
        }
    }

    override suspend fun stopStreamingTranscription() {
        isStreaming = false
        whisperProcessor.stopStreamingTranscription()
    }

    override suspend fun translateText(
        text: String, 
        sourceLanguage: String, 
        targetLanguage: String
    ): String = withContext(Dispatchers.IO) {
        try {
            translationProcessor.translate(text, sourceLanguage, targetLanguage)
        } catch (e: Exception) {
            Log.e(TAG, "Error translating text", e)
            text // Return original text on error
        }
    }

    override suspend fun synthesizeSpeech(text: String, voiceEmbeddingId: String?): ByteArray = withContext(Dispatchers.IO) {
        try {
            val embedding = voiceEmbeddingId?.let {
                voiceEmbeddingRepository.getEmbeddingById(it)?.embeddingData
            }
            
            ttsProcessor.synthesize(text, embedding)
        } catch (e: Exception) {
            Log.e(TAG, "Error synthesizing speech", e)
            ByteArray(0) // Return empty array on error
        }
    }

    override suspend fun startStreamingSynthesis(
        textFlow: Flow<String>, 
        voiceEmbeddingId: String?
    ): Flow<ByteArray> {
        return textFlow.map { text ->
            synthesizeSpeech(text, voiceEmbeddingId)
        }
    }
}
