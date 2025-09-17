package com.example.verbyflow.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.verbyflow.domain.model.VoiceEmbedding
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

private val Context.embeddingDataStore by preferencesDataStore(name = "embedding_preferences")

private const val TAG = "EmbeddingPreferences"

@Singleton
class EmbeddingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val EMBEDDING_IDS = stringPreferencesKey("embedding_ids")
        
        fun embeddingKey(id: String) = stringPreferencesKey("embedding_$id")
        fun embeddingUserIdKey(id: String) = stringPreferencesKey("embedding_user_$id")
        fun embeddingCreatedKey(id: String) = longPreferencesKey("embedding_created_$id")
        fun embeddingLastUsedKey(id: String) = longPreferencesKey("embedding_lastused_$id")
    }
    
    suspend fun saveEmbedding(embedding: VoiceEmbedding) {
        // Convert embedding data to Base64 string
        val byteArray = ByteArrayOutputStream().use { byteOutput ->
            DataOutputStream(byteOutput).use { dataOutput ->
                embedding.embeddingData.forEach { dataOutput.writeFloat(it) }
            }
            byteOutput.toByteArray()
        }
        
        val encodedEmbedding = Base64.getEncoder().encodeToString(byteArray)
        
        // Save to preferences
        context.embeddingDataStore.edit { preferences ->
            // Add ID to list of embeddings
            val existingIds = preferences[PreferencesKeys.EMBEDDING_IDS]?.split(",") ?: emptyList()
            val updatedIds = if (existingIds.contains(embedding.id)) {
                existingIds
            } else {
                existingIds + embedding.id
            }
            preferences[PreferencesKeys.EMBEDDING_IDS] = updatedIds.joinToString(",")
            
            // Save embedding data
            preferences[PreferencesKeys.embeddingKey(embedding.id)] = encodedEmbedding
            preferences[PreferencesKeys.embeddingUserIdKey(embedding.id)] = embedding.userId
            preferences[PreferencesKeys.embeddingCreatedKey(embedding.id)] = embedding.createdAt
            preferences[PreferencesKeys.embeddingLastUsedKey(embedding.id)] = embedding.lastUsedAt
        }
    }
    
    suspend fun getEmbeddingById(id: String): VoiceEmbedding? {
        return context.embeddingDataStore.data.map { preferences ->
            val encodedEmbedding = preferences[PreferencesKeys.embeddingKey(id)] ?: return@map null
            val userId = preferences[PreferencesKeys.embeddingUserIdKey(id)] ?: return@map null
            val created = preferences[PreferencesKeys.embeddingCreatedKey(id)] ?: System.currentTimeMillis()
            val lastUsed = preferences[PreferencesKeys.embeddingLastUsedKey(id)] ?: System.currentTimeMillis()
            
            try {
                // Decode Base64 string to float array
                val embeddingBytes = Base64.getDecoder().decode(encodedEmbedding)
                val embeddingData = ByteArrayInputStream(embeddingBytes).use { byteInput ->
                    DataInputStream(byteInput).use { dataInput ->
                        val floatCount = embeddingBytes.size / 4
                        FloatArray(floatCount) { dataInput.readFloat() }
                    }
                }
                
                VoiceEmbedding(
                    id = id,
                    userId = userId,
                    embeddingData = embeddingData,
                    createdAt = created,
                    lastUsedAt = lastUsed
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding voice embedding", e)
                null
            }
        }.firstOrNull()
    }
    
    suspend fun getEmbeddingForUser(userId: String): VoiceEmbedding? {
        return getAllEmbeddings().find { it.userId == userId }
    }
    
    suspend fun getAllEmbeddings(): List<VoiceEmbedding> {
        val embeddings = mutableListOf<VoiceEmbedding>()
        
        context.embeddingDataStore.data.map { preferences ->
            val embeddingIds = preferences[PreferencesKeys.EMBEDDING_IDS]?.split(",") ?: emptyList()
            embeddingIds.forEach { id ->
                if (id.isNotEmpty()) {
                    getEmbeddingById(id)?.let { embeddings.add(it) }
                }
            }
        }.firstOrNull()
        
        return embeddings
    }
    
    suspend fun deleteEmbedding(embeddingId: String) {
        context.embeddingDataStore.edit { preferences ->
            // Remove ID from list of embeddings
            val existingIds = preferences[PreferencesKeys.EMBEDDING_IDS]?.split(",") ?: emptyList()
            val updatedIds = existingIds.filter { it != embeddingId }
            
            if (updatedIds.isEmpty()) {
                preferences.remove(PreferencesKeys.EMBEDDING_IDS)
            } else {
                preferences[PreferencesKeys.EMBEDDING_IDS] = updatedIds.joinToString(",")
            }
            
            // Remove embedding data
            preferences.remove(PreferencesKeys.embeddingKey(embeddingId))
            preferences.remove(PreferencesKeys.embeddingUserIdKey(embeddingId))
            preferences.remove(PreferencesKeys.embeddingCreatedKey(embeddingId))
            preferences.remove(PreferencesKeys.embeddingLastUsedKey(embeddingId))
        }
    }
}
