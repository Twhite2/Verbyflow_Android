package com.example.verbyflow.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.verbyflow.domain.model.User
import com.example.verbyflow.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class UserRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserRepository {

    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val VOICE_EMBEDDING_ID = stringPreferencesKey("voice_embedding_id")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
    }

    override suspend fun getCurrentUser(): User? {
        return context.userDataStore.data.map { preferences ->
            val userId = preferences[PreferencesKeys.USER_ID] ?: return@map null
            User(
                id = userId,
                name = preferences[PreferencesKeys.USER_NAME] ?: "",
                voiceEmbeddingId = preferences[PreferencesKeys.VOICE_EMBEDDING_ID],
                preferredLanguage = preferences[PreferencesKeys.PREFERRED_LANGUAGE] ?: "en",
                isOnboarded = preferences[PreferencesKeys.IS_ONBOARDED] ?: false
            )
        }.collect { user -> 
            return user
        }
        
        return null
    }

    override suspend fun saveUser(user: User) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = user.id
            preferences[PreferencesKeys.USER_NAME] = user.name
            user.voiceEmbeddingId?.let { preferences[PreferencesKeys.VOICE_EMBEDDING_ID] = it }
            preferences[PreferencesKeys.PREFERRED_LANGUAGE] = user.preferredLanguage
            preferences[PreferencesKeys.IS_ONBOARDED] = user.isOnboarded
        }
    }

    override suspend fun updateUserOnboardingStatus(userId: String, isOnboarded: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDED] = isOnboarded
        }
    }

    override suspend fun updateUserVoiceEmbeddingId(userId: String, embeddingId: String?) {
        context.userDataStore.edit { preferences ->
            if (embeddingId != null) {
                preferences[PreferencesKeys.VOICE_EMBEDDING_ID] = embeddingId
            } else {
                preferences.remove(PreferencesKeys.VOICE_EMBEDDING_ID)
            }
        }
    }

    override suspend fun updateUserPreferredLanguage(userId: String, language: String) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_LANGUAGE] = language
        }
    }

    override fun observeCurrentUser(): Flow<User?> {
        return context.userDataStore.data.map { preferences ->
            val userId = preferences[PreferencesKeys.USER_ID] ?: return@map null
            User(
                id = userId,
                name = preferences[PreferencesKeys.USER_NAME] ?: "",
                voiceEmbeddingId = preferences[PreferencesKeys.VOICE_EMBEDDING_ID],
                preferredLanguage = preferences[PreferencesKeys.PREFERRED_LANGUAGE] ?: "en",
                isOnboarded = preferences[PreferencesKeys.IS_ONBOARDED] ?: false
            )
        }
    }
}
