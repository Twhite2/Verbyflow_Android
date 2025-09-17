package com.example.verbyflow.ml

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WhisperProcessor"
private const val MODEL_FILE = "whisper_small.tflite"
private const val SAMPLE_RATE = 16000
private const val FRAME_SIZE = 160 // 10ms at 16kHz
private const val BUFFER_SIZE_SECONDS = 30
private const val AUDIO_BUFFER_SIZE = SAMPLE_RATE * BUFFER_SIZE_SECONDS * 2 // 16-bit samples

/**
 * Processes audio for transcription using Whisper Small model
 */
@Singleton
class WhisperProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    private val isProcessing = AtomicBoolean(false)
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var processingJob: Job? = null
    
    private val audioBuffer = ByteBuffer.allocateDirect(AUDIO_BUFFER_SIZE)
    private val audioQueue = ConcurrentLinkedQueue<ByteBuffer>()
    
    private var audioRecord: AudioRecord? = null
    
    interface TranscriptionListener {
        fun onTranscription(text: String)
        fun onError(error: String)
    }
    
    init {
        audioBuffer.order(ByteOrder.nativeOrder())
        try {
            loadModel()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Whisper model", e)
        }
    }
    
    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            isModelLoaded = true
            Log.d(TAG, "Whisper model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Could not load Whisper model", e)
            throw e
        }
    }
    
    private fun loadModelFile(): MappedByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Transcribe audio data
     */
    fun transcribe(audioData: ByteArray, language: String = "en"): String {
        if (!isModelLoaded) {
            loadModel()
        }
        
        // Prepare input buffer
        val inputBuffer = prepareInputBuffer(audioData)
        
        // Prepare output buffer for the transcription text
        // This is a simplified version - in reality, the output handling depends on the specific model output format
        val outputSize = 256 // Arbitrary size for output text buffer
        val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4) // 4 bytes per float
        outputBuffer.order(ByteOrder.nativeOrder())
        
        // Run inference
        synchronized(this) {
            val inputs = mapOf("audio" to inputBuffer, "language" to language)
            val outputs = mapOf("text" to outputBuffer)
            interpreter?.runForMultipleInputsOutputs(inputs, outputs)
        }
        
        // Process the output to get the transcribed text
        // This is a placeholder - actual implementation depends on the model's output format
        return "Transcription result"
    }
    
    /**
     * Start streaming transcription
     */
    fun startStreamingTranscription(language: String = "en", listener: TranscriptionListener) {
        if (!isModelLoaded) {
            try {
                loadModel()
            } catch (e: Exception) {
                listener.onError("Failed to load Whisper model: ${e.message}")
                return
            }
        }
        
        if (isProcessing.getAndSet(true)) {
            listener.onError("Streaming transcription already in progress")
            return
        }
        
        try {
            // Initialize audio recording
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )
            
            audioRecord?.startRecording()
            
            // Start processing loop
            processingJob = coroutineScope.launch {
                val buffer = ShortArray(FRAME_SIZE)
                var lastTranscription = ""
                var accumulatedAudio = ByteBuffer.allocateDirect(SAMPLE_RATE * 5 * 2) // 5 seconds buffer
                accumulatedAudio.order(ByteOrder.nativeOrder())
                
                while (isProcessing.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (read > 0) {
                        // Add audio data to buffer
                        for (i in 0 until read) {
                            if (accumulatedAudio.remaining() < 2) {
                                // Process buffer when full
                                accumulatedAudio.flip()
                                val audioBytes = ByteArray(accumulatedAudio.remaining())
                                accumulatedAudio.get(audioBytes)
                                
                                val transcription = transcribe(audioBytes, language)
                                if (transcription != lastTranscription) {
                                    lastTranscription = transcription
                                    listener.onTranscription(transcription)
                                }
                                
                                // Reset buffer for new data
                                accumulatedAudio.clear()
                            }
                            
                            accumulatedAudio.putShort(buffer[i])
                        }
                    }
                    
                    // Process partial buffer every second or so
                    if (accumulatedAudio.position() > SAMPLE_RATE * 2) { // 1 second worth of data
                        val position = accumulatedAudio.position()
                        accumulatedAudio.flip()
                        val audioBytes = ByteArray(accumulatedAudio.remaining())
                        accumulatedAudio.get(audioBytes)
                        
                        val transcription = transcribe(audioBytes, language)
                        if (transcription != lastTranscription) {
                            lastTranscription = transcription
                            listener.onTranscription(transcription)
                        }
                        
                        accumulatedAudio.clear()
                    }
                }
            }
        } catch (e: Exception) {
            stopStreamingTranscription()
            listener.onError("Error starting transcription: ${e.message}")
        }
    }
    
    /**
     * Stop streaming transcription
     */
    fun stopStreamingTranscription() {
        isProcessing.set(false)
        processingJob?.cancel()
        processingJob = null
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        audioQueue.clear()
    }
    
    /**
     * Prepare input buffer from audio data
     */
    private fun prepareInputBuffer(audioData: ByteArray): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(audioData.size)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.put(audioData)
        inputBuffer.flip()
        return inputBuffer
    }
    
    /**
     * Release resources
     */
    fun release() {
        stopStreamingTranscription()
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}
