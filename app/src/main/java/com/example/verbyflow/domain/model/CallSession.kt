package com.example.verbyflow.domain.model

import java.util.UUID

/**
 * Represents a call session between two users.
 */
data class CallSession(
    val id: String = UUID.randomUUID().toString(),
    val initiatorId: String,
    val receiverId: String? = null,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val sourceLanguage: String = "en",
    val targetLanguage: String = "en",
    val status: CallStatus = CallStatus.INITIATING
)

enum class CallStatus {
    INITIATING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED
}
