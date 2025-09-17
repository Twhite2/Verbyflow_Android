package com.example.verbyflow.data.repository

import android.util.Log
import com.example.verbyflow.data.remote.WebRTCClient
import com.example.verbyflow.domain.model.CallSession
import com.example.verbyflow.domain.model.CallStatus
import com.example.verbyflow.domain.model.User
import com.example.verbyflow.domain.repository.CallRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CallRepositoryImpl"

@Singleton
class CallRepositoryImpl @Inject constructor(
    private val webRTCClient: WebRTCClient
) : CallRepository {

    private val _currentCallSession = MutableStateFlow<CallSession?>(null)
    
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )
    
    override suspend fun initializeWebRTC() = withContext(Dispatchers.IO) {
        webRTCClient.initialize()
        webRTCClient.createPeerConnection(iceServers)
        // No need to return anything as per the interface
    }

    override suspend fun createCallOffer(initiator: User): SessionDescription = withContext(Dispatchers.IO) {
        val callSession = CallSession(
            initiatorId = initiator.id,
            sourceLanguage = initiator.preferredLanguage,
            status = CallStatus.INITIATING
        )
        _currentCallSession.value = callSession
        
        var sessionDescription: SessionDescription? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        
        webRTCClient.createOffer { sdp ->
            sessionDescription = sdp
            latch.countDown()
        }
        
        latch.await()
        
        if (sessionDescription == null) {
            throw IllegalStateException("Failed to create call offer")
        }
        
        _currentCallSession.value = callSession.copy(status = CallStatus.CONNECTING)
        sessionDescription!!
    }

    override suspend fun acceptCall(callSession: CallSession): SessionDescription = withContext(Dispatchers.IO) {
        _currentCallSession.value = callSession.copy(status = CallStatus.CONNECTING)
        
        var sessionDescription: SessionDescription? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        
        webRTCClient.createAnswer { sdp ->
            sessionDescription = sdp
            latch.countDown()
        }
        
        latch.await()
        
        if (sessionDescription == null) {
            throw IllegalStateException("Failed to create answer")
        }
        
        _currentCallSession.value = callSession.copy(status = CallStatus.CONNECTED)
        sessionDescription!!
    }

    override suspend fun rejectCall(callSession: CallSession) {
        _currentCallSession.value = callSession.copy(
            status = CallStatus.DISCONNECTED,
            endTime = System.currentTimeMillis()
        )
        webRTCClient.dispose()
    }

    override suspend fun endCall(callSession: CallSession) {
        _currentCallSession.value = callSession.copy(
            status = CallStatus.DISCONNECTED,
            endTime = System.currentTimeMillis()
        )
        webRTCClient.dispose()
    }

    override suspend fun addIceCandidate(iceCandidate: String) {
        try {
            val parts = iceCandidate.split("|")
            if (parts.size == 3) {
                val candidate = IceCandidate(parts[0], parts[1].toInt(), parts[2])
                webRTCClient.addIceCandidate(candidate)
            } else {
                Log.e(TAG, "Invalid ice candidate format: $iceCandidate")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding ice candidate", e)
        }
    }

    override suspend fun startAudioCapture() {
        webRTCClient.startAudioCapture()
    }

    override suspend fun stopAudioCapture() {
        webRTCClient.stopAudioCapture()
    }

    override fun observeCallState(): Flow<CallSession?> = _currentCallSession

    override fun observeRemoteStream(): Flow<ByteArray?> = webRTCClient.remoteAudioFlow.map { buffer ->
        buffer?.let {
            val bytes = ByteArray(it.remaining())
            it.get(bytes)
            bytes
        }
    }
}
