package com.example.verbyflow.domain.model

import java.util.UUID

/**
 * Represents a user in the VerbyFlow system.
 */
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val voiceEmbeddingId: String? = null,
    val preferredLanguage: String = "en",
    val isOnboarded: Boolean = false
)
