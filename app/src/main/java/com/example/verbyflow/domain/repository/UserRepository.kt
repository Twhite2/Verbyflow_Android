package com.example.verbyflow.domain.repository

import com.example.verbyflow.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user-related operations.
 */
interface UserRepository {
    suspend fun getCurrentUser(): User?
    suspend fun saveUser(user: User)
    suspend fun updateUserOnboardingStatus(userId: String, isOnboarded: Boolean)
    suspend fun updateUserVoiceEmbeddingId(userId: String, embeddingId: String?)
    suspend fun updateUserPreferredLanguage(userId: String, language: String)
    fun observeCurrentUser(): Flow<User?>
}
