package com.example.verbyflow.domain.repository

import com.example.verbyflow.domain.model.VoiceEmbedding
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for voice embedding operations.
 */
interface VoiceEmbeddingRepository {
    suspend fun saveVoiceEmbedding(embedding: VoiceEmbedding): String
    suspend fun getEmbeddingById(id: String): VoiceEmbedding?
    suspend fun getEmbeddingForUser(userId: String): VoiceEmbedding?
    suspend fun generateEmbedding(audioFile: File, userId: String): VoiceEmbedding
    suspend fun deleteEmbedding(embeddingId: String)
    fun observeUserEmbedding(userId: String): Flow<VoiceEmbedding?>
}
