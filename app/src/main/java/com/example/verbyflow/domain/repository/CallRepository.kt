package com.example.verbyflow.domain.repository

import com.example.verbyflow.domain.model.CallSession
import com.example.verbyflow.domain.model.User
import kotlinx.coroutines.flow.Flow
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

/**
 * Repository interface for WebRTC call operations.
 */
interface CallRepository {
    suspend fun initializeWebRTC()
    suspend fun createCallOffer(initiator: User): SessionDescription
    suspend fun acceptCall(callSession: CallSession): SessionDescription
    suspend fun rejectCall(callSession: CallSession)
    suspend fun endCall(callSession: CallSession)
    suspend fun addIceCandidate(iceCandidate: String)
    suspend fun startAudioCapture()
    suspend fun stopAudioCapture()
    fun observeCallState(): Flow<CallSession?>
    fun observeRemoteStream(): Flow<ByteArray?>
}
