package com.example.verbyflow.domain.usecase.onboarding

import com.example.verbyflow.domain.model.VoiceEmbedding
import com.example.verbyflow.domain.repository.UserRepository
import com.example.verbyflow.domain.repository.VoiceEmbeddingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Use case for generating voice embedding during onboarding.
 */
class GenerateVoiceEmbeddingUseCase @Inject constructor(
    private val voiceEmbeddingRepository: VoiceEmbeddingRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(audioFile: File): Result<VoiceEmbedding> = withContext(Dispatchers.IO) {
        try {
            val currentUser = userRepository.getCurrentUser() ?: return@withContext Result.failure(
                IllegalStateException("No current user found")
            )
            
            val embedding = voiceEmbeddingRepository.generateEmbedding(audioFile, currentUser.id)
            val embeddingId = voiceEmbeddingRepository.saveVoiceEmbedding(embedding)
            
            // Update user with new embedding ID
            userRepository.updateUserVoiceEmbeddingId(currentUser.id, embeddingId)
            userRepository.updateUserOnboardingStatus(currentUser.id, true)
            
            Result.success(embedding)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
