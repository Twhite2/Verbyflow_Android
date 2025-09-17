package com.example.verbyflow.domain.model

import java.util.UUID

/**
 * Represents a voice embedding for a user created using ECAPA-TDNN.
 */
data class VoiceEmbedding(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val embeddingData: FloatArray,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoiceEmbedding

        if (id != other.id) return false
        if (userId != other.userId) return false
        if (!embeddingData.contentEquals(other.embeddingData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + embeddingData.contentHashCode()
        return result
    }
}
