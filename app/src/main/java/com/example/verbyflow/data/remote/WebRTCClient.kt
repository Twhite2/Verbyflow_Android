package com.example.verbyflow.data.remote

import android.content.Context
import android.media.AudioRecord
import android.media.AudioTrack as AndroidAudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WebRTCClient"

@Singleton
class WebRTCClient @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // PeerConnection factory
    private var factory: PeerConnectionFactory? = null
    
    // PeerConnection
    private var peerConnection: PeerConnection? = null
    
    // Audio source and track
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    
    // State flows
    private val _connectionState = MutableStateFlow<PeerConnection.SignalingState?>(null)
    val connectionState: StateFlow<PeerConnection.SignalingState?> = _connectionState
    
    private val _remoteAudioFlow = MutableStateFlow<ByteBuffer?>(null)
    val remoteAudioFlow: StateFlow<ByteBuffer?> = _remoteAudioFlow
    
    // Audio processing
    private var audioRecorder: AudioRecord? = null
    private var audioConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
    }
    
    // Custom class to handle audio data from WebRTC
    private inner class AudioDataObserver {
        fun onWebRtcAudioData(audioSamples: ByteBuffer, bitsPerSample: Int, sampleRate: Int, channels: Int, frames: Int) {
            // Make a copy of the buffer to avoid modification issues
            val copy = ByteBuffer.allocate(audioSamples.remaining())
            val position = audioSamples.position()
            copy.put(audioSamples)
            // Reset position of original buffer
            audioSamples.position(position)
            copy.flip()
            
            _remoteAudioFlow.value = copy
        }
    }
    
    // Initialize WebRTC components
    fun initialize() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
            
        PeerConnectionFactory.initialize(options)
        
        // Create AudioDeviceModule with recording but no playout
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()
            
        // Create factory
        val encoderFactory = SoftwareVideoEncoderFactory()
        val decoderFactory = SoftwareVideoDecoderFactory()
        
        factory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
            
        Log.d(TAG, "PeerConnectionFactory initialized")
    }
    
    // Create peer connection
    fun createPeerConnection(iceServers: List<PeerConnection.IceServer>): PeerConnection? {
        if (factory == null) {
            Log.e(TAG, "PeerConnectionFactory is not initialized")
            return null
        }
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
        }
        
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {
                Log.d(TAG, "onSignalingChange: $signalingState")
                _connectionState.value = signalingState
            }
            
            override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: $iceConnectionState")
            }
            
            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "onIceConnectionReceivingChange: $receiving")
            }
            
            override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: $iceGatheringState")
            }
            
            override fun onIceCandidate(iceCandidate: IceCandidate) {
                Log.d(TAG, "onIceCandidate: $iceCandidate")
                // Send this to signaling server
            }
            
            override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>) {
                Log.d(TAG, "onIceCandidatesRemoved")
            }
            
            override fun onAddStream(mediaStream: MediaStream) {
                Log.d(TAG, "onAddStream")
                // Handle remote media stream
                mediaStream.audioTracks.forEach { track ->
                    track.setEnabled(true)
                    // Simply log the audio track info since we can't directly access the raw audio data
                    Log.d(TAG, "Added audio track: ${track.id()}")
                    // We'll use the standard Android audio APIs instead
                }
            }
            
            override fun onRemoveStream(mediaStream: MediaStream) {
                Log.d(TAG, "onRemoveStream")
            }
            
            override fun onDataChannel(dataChannel: DataChannel) {
                Log.d(TAG, "onDataChannel")
            }
            
            override fun onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded")
            }
            
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
                Log.d(TAG, "onAddTrack")
            }
        }
        
        peerConnection = factory?.createPeerConnection(rtcConfig, observer)
        Log.d(TAG, "PeerConnection created")
        
        return peerConnection
    }
    
    // Start audio capture
    fun startAudioCapture() {
        if (factory == null) {
            Log.e(TAG, "PeerConnectionFactory is not initialized")
            return
        }
        
        audioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack("audio-track-${UUID.randomUUID()}", audioSource)
        
        localAudioTrack?.setEnabled(true)
        
        val stream = factory?.createLocalMediaStream("local-stream-${UUID.randomUUID()}")
        stream?.addTrack(localAudioTrack)
        
        peerConnection?.addTrack(localAudioTrack, listOf(stream?.id))
        
        Log.d(TAG, "Audio capture started")
    }
    
    // Stop audio capture
    fun stopAudioCapture() {
        audioSource?.dispose()
        audioSource = null
        
        localAudioTrack?.setEnabled(false)
        localAudioTrack?.dispose()
        localAudioTrack = null
        
        Log.d(TAG, "Audio capture stopped")
    }
    
    // Create offer
    fun createOffer(callback: (SessionDescription?) -> Unit) {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d(TAG, "Offer created successfully")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local description set successfully")
                        callback(sessionDescription)
                    }
                    
                    override fun onCreateFailure(p0: String?) {
                        Log.e(TAG, "Create local description failed: $p0")
                        callback(null)
                    }
                    
                    override fun onSetFailure(p0: String?) {
                        Log.e(TAG, "Set local description failed: $p0")
                        callback(null)
                    }
                }, sessionDescription)
            }
            
            override fun onSetSuccess() {}
            
            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "Create offer failed: $p0")
                callback(null)
            }
            
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }
    
    // Accept remote offer
    fun setRemoteDescription(sessionDescription: SessionDescription, callback: (Boolean) -> Unit) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set successfully")
                callback(true)
            }
            
            override fun onCreateFailure(p0: String?) {}
            
            override fun onSetFailure(p0: String?) {
                Log.e(TAG, "Set remote description failed: $p0")
                callback(false)
            }
        }, sessionDescription)
    }
    
    // Create answer
    fun createAnswer(callback: (SessionDescription?) -> Unit) {
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d(TAG, "Answer created successfully")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local description set successfully")
                        callback(sessionDescription)
                    }
                    
                    override fun onCreateFailure(p0: String?) {
                        Log.e(TAG, "Create local description failed: $p0")
                        callback(null)
                    }
                    
                    override fun onSetFailure(p0: String?) {
                        Log.e(TAG, "Set local description failed: $p0")
                        callback(null)
                    }
                }, sessionDescription)
            }
            
            override fun onSetSuccess() {}
            
            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "Create answer failed: $p0")
                callback(null)
            }
            
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }
    
    // Add ice candidate
    fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }
    
    // Dispose resources
    fun dispose() {
        stopAudioCapture()
        
        try {
            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null
            
            factory?.dispose()
            factory = null
            
            _connectionState.value = null
            _remoteAudioFlow.value = null
            
            Log.d(TAG, "WebRTC resources disposed")
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing WebRTC resources", e)
        }
    }
}
