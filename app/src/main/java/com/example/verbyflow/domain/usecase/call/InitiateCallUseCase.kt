package com.example.verbyflow.domain.usecase.call

import com.example.verbyflow.domain.model.CallSession
import com.example.verbyflow.domain.repository.CallRepository
import com.example.verbyflow.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for initiating a WebRTC call.
 */
class InitiateCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(receiverId: String): Result<CallSession> = withContext(Dispatchers.IO) {
        try {
            val currentUser = userRepository.getCurrentUser() ?: return@withContext Result.failure(
                IllegalStateException("No current user found")
            )
            
            // Initialize WebRTC
            callRepository.initializeWebRTC()
            
            // Create call session
            val callSession = CallSession(
                initiatorId = currentUser.id,
                receiverId = receiverId,
                sourceLanguage = currentUser.preferredLanguage
            )
            
            // Create call offer
            callRepository.createCallOffer(currentUser)
            
            // Start audio capture
            callRepository.startAudioCapture()
            
            Result.success(callSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
