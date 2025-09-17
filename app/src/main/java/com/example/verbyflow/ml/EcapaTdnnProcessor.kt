package com.example.verbyflow.ml

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "EcapaTdnnProcessor"
private const val MODEL_FILE = "ecapa_tdnn_embedding.tflite"
private const val SAMPLE_RATE = 16000
private const val EMBEDDING_SIZE = 192 // ECAPA-TDNN typically produces embeddings of this size

/**
 * Processor for ECAPA-TDNN voice embedding model
 */
@Singleton
class EcapaTdnnProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    
    init {
        try {
            loadModel()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ECAPA-TDNN model", e)
        }
    }
    
    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
            isModelLoaded = true
            Log.d(TAG, "ECAPA-TDNN model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Could not load ECAPA-TDNN model", e)
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
     * Process audio file to generate voice embedding
     */
    suspend fun processAudio(audioFile: File): FloatArray {
        if (!isModelLoaded) {
            loadModel()
        }
        
        // Load and preprocess audio file
        val audioData = loadAndPreprocessAudio(audioFile)
        
        // Run inference
        val outputBuffer = ByteBuffer.allocateDirect(EMBEDDING_SIZE * Float.SIZE_BYTES)
        outputBuffer.order(ByteOrder.nativeOrder())
        
        synchronized(this) {
            interpreter?.run(audioData, outputBuffer)
        }
        
        // Convert output to float array
        outputBuffer.rewind()
        val outputArray = FloatArray(EMBEDDING_SIZE)
        for (i in 0 until EMBEDDING_SIZE) {
            outputArray[i] = outputBuffer.getFloat()
        }
        
        // Normalize embedding (optional but recommended)
        return normalizeEmbedding(outputArray)
    }
    
    /**
     * Load and preprocess audio file for the model
     */
    private fun loadAndPreprocessAudio(audioFile: File): ByteBuffer {
        // For a real implementation, you'd use proper audio loading and processing here
        // This is a simplified version just for illustration
        
        // Assuming 5-10 seconds of audio at 16kHz = ~80k-160k samples
        // ECAPA-TDNN usually works with features like MFCCs, but for simplicity,
        // we'll just use raw audio in this example
        val maxSamples = 160000 // 10 seconds at 16kHz
        val inputBuffer = ByteBuffer.allocateDirect(maxSamples * 2) // 16-bit samples
        inputBuffer.order(ByteOrder.nativeOrder())
        
        // Read audio data from file and convert to model input format
        // This is placeholder code - in a real app, use proper audio loading library
        // and feature extraction
        
        // Dummy placeholder - in real implementation, load actual audio data
        // and convert to appropriate model input format
        val dummyData = ShortArray(maxSamples)
        inputBuffer.asShortBuffer().put(dummyData)
        
        return inputBuffer
    }
    
    /**
     * Normalize embedding vector (L2 normalization)
     */
    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        var sum = 0f
        for (value in embedding) {
            sum += value * value
        }
        
        val norm = kotlin.math.sqrt(sum)
        for (i in embedding.indices) {
            embedding[i] /= norm
        }
        
        return embedding
    }
    
    /**
     * Record audio from microphone
     */
    fun recordAudio(durationMs: Int): File {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        
        val outputFile = File(context.cacheDir, "voice_sample_${System.currentTimeMillis()}.pcm")
        outputFile.createNewFile()
        
        audioRecord.startRecording()
        
        try {
            // Calculate number of samples based on duration
            val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val audioBuffer = ShortArray(bufferSize / 2)
            
            outputFile.outputStream().use { outputStream ->
                var samplesRead = 0
                while (samplesRead < totalSamples) {
                    val read = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                    if (read > 0) {
                        // Write samples to file
                        for (i in 0 until read) {
                            val bytes = ByteArray(2)
                            bytes[0] = audioBuffer[i].toByte()
                            bytes[1] = (audioBuffer[i].toInt() shr 8).toByte()
                            outputStream.write(bytes)
                        }
                        samplesRead += read
                    }
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
        
        return outputFile
    }
    
    /**
     * Compare two embeddings to determine if they are from the same speaker
     * Returns a confidence score (0-1) where higher means more likely same speaker
     */
    fun compareSpeakerEmbeddings(embedding1: FloatArray, embedding2: FloatArray): Float {
        // Compute cosine similarity
        var dotProduct = 0f
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
        }
        
        // Cosine similarity ranges from -1 to 1, normalize to 0-1
        return (dotProduct + 1) / 2
    }
    
    fun release() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}
