package com.example.verbyflow.data.repository

import android.content.Context
import android.util.Log
import com.example.verbyflow.data.local.EmbeddingPreferences
import com.example.verbyflow.domain.model.VoiceEmbedding
import com.example.verbyflow.domain.repository.VoiceEmbeddingRepository
import com.example.verbyflow.ml.EcapaTdnnProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VoiceEmbeddingRepo"

@Singleton
class VoiceEmbeddingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val embeddingPreferences: EmbeddingPreferences,
    private val ecapaTdnnProcessor: EcapaTdnnProcessor
) : VoiceEmbeddingRepository {

    private val embeddingsCache = mutableMapOf<String, VoiceEmbedding>()
    private val userEmbeddingFlow = MutableStateFlow<Map<String, VoiceEmbedding>>(mapOf())

    override suspend fun saveVoiceEmbedding(embedding: VoiceEmbedding): String = withContext(Dispatchers.IO) {
        try {
            embeddingPreferences.saveEmbedding(embedding)
            embeddingsCache[embedding.id] = embedding
            
            val userEmbeddings = userEmbeddingFlow.value.toMutableMap()
            userEmbeddings[embedding.userId] = embedding
            userEmbeddingFlow.value = userEmbeddings
            
            embedding.id
        } catch (e: Exception) {
            Log.e(TAG, "Error saving voice embedding", e)
            throw e
        }
    }

    override suspend fun getEmbeddingById(id: String): VoiceEmbedding? = withContext(Dispatchers.IO) {
        embeddingsCache[id] ?: embeddingPreferences.getEmbeddingById(id)?.also {
            embeddingsCache[id] = it
        }
    }

    override suspend fun getEmbeddingForUser(userId: String): VoiceEmbedding? = withContext(Dispatchers.IO) {
        embeddingPreferences.getEmbeddingForUser(userId)?.also {
            embeddingsCache[it.id] = it
            
            val userEmbeddings = userEmbeddingFlow.value.toMutableMap()
            userEmbeddings[userId] = it
            userEmbeddingFlow.value = userEmbeddings
        }
    }

    override suspend fun generateEmbedding(audioFile: File, userId: String): VoiceEmbedding = withContext(Dispatchers.IO) {
        try {
            val embeddingData = ecapaTdnnProcessor.processAudio(audioFile)
            VoiceEmbedding(
                userId = userId,
                embeddingData = embeddingData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating voice embedding", e)
            throw e
        }
    }

    override suspend fun deleteEmbedding(embeddingId: String) = withContext(Dispatchers.IO) {
        embeddingPreferences.deleteEmbedding(embeddingId)
        embeddingsCache.remove(embeddingId)
        
        val embedding = embeddingPreferences.getAllEmbeddings().find { it.id == embeddingId }
        if (embedding != null) {
            val userEmbeddings = userEmbeddingFlow.value.toMutableMap()
            userEmbeddings.remove(embedding.userId)
            userEmbeddingFlow.value = userEmbeddings
        }
    }

    override fun observeUserEmbedding(userId: String): Flow<VoiceEmbedding?> {
        return userEmbeddingFlow.map { it[userId] }
    }
}
