package com.example.verbyflow.domain.usecase.call

import com.example.verbyflow.domain.repository.SpeechRepository
import com.example.verbyflow.domain.repository.UserRepository
import com.example.verbyflow.domain.repository.VoiceEmbeddingRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Use case for processing speech through the pipeline:
 * Audio -> STT -> Translation -> TTS -> Output
 */
class ProcessSpeechUseCase @Inject constructor(
    private val speechRepository: SpeechRepository,
    private val userRepository: UserRepository,
    private val voiceEmbeddingRepository: VoiceEmbeddingRepository
) {
    suspend operator fun invoke(targetLanguage: String): Flow<ProcessingResult> {
        val user = userRepository.getCurrentUser() ?: throw IllegalStateException("No user found")
        val sourceLanguage = user.preferredLanguage
        
        return speechRepository.startStreamingTranscription(sourceLanguage)
            .map { transcription ->
                emit(ProcessingResult.Transcription(transcription))
                
                // Translate the text
                val translatedText = speechRepository.translateText(
                    text = transcription,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage
                )
                emit(ProcessingResult.Translation(translatedText))
                
                // Retrieve voice embedding if available
                val voiceEmbedding = user.voiceEmbeddingId?.let {
                    voiceEmbeddingRepository.getEmbeddingById(it)
                }
                
                // Synthesize speech with the user's voice
                val audioBytes = speechRepository.synthesizeSpeech(
                    text = translatedText,
                    voiceEmbeddingId = voiceEmbedding?.id
                )
                
                ProcessingResult.SynthesizedSpeech(audioBytes)
            }
    }
    
    sealed class ProcessingResult {
        data class Transcription(val text: String) : ProcessingResult()
        data class Translation(val text: String) : ProcessingResult()
        data class SynthesizedSpeech(val audioData: ByteArray) : ProcessingResult() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as SynthesizedSpeech

                return audioData.contentEquals(other.audioData)
            }

            override fun hashCode(): Int {
                return audioData.contentHashCode()
            }
        }
    }
}
