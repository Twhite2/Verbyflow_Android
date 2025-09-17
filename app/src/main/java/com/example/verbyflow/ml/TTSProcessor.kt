package com.example.verbyflow.ml

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TTSProcessor"
private const val MODEL_FILE = "dia_tts.tflite"
private const val MAX_TEXT_LENGTH = 256
private const val EMBEDDING_SIZE = 192 // Must match ECAPA-TDNN embedding size
private const val AUDIO_SAMPLE_RATE = 24000 // DIA-TTS typically outputs at 24kHz

/**
 * Handles text-to-speech synthesis with voice cloning using the DIA-TTS model
 */
@Singleton
class TTSProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    
    // Text processing components
    private val symbols = listOf(" ", "!", "\"", "#", "$", "%", "&", "'", "(", ")", "*", "+", ",", 
        "-", ".", "/", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ":", ";", "<", "=", ">", 
        "?", "@", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", 
        "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "[", "\\", "]", "^", "_", "`", "a", "b", 
        "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", 
        "u", "v", "w", "x", "y", "z", "{", "|", "}", "~")
    
    private val symbolToId = symbols.mapIndexed { index, symbol -> symbol to index }.toMap()
    
    init {
        try {
            loadModel()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TTS model", e)
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
            Log.d(TAG, "TTS model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Could not load TTS model", e)
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
     * Synthesize speech from text, optionally using a voice embedding for cloning
     */
    fun synthesize(text: String, voiceEmbedding: FloatArray? = null): ByteArray {
        if (!isModelLoaded) {
            loadModel()
        }
        
        // Process input text
        val cleanedText = cleanText(text)
        val textIds = encodeText(cleanedText)
        
        // Prepare input buffers
        val textBuffer = ByteBuffer.allocateDirect(MAX_TEXT_LENGTH * 4) // 4 bytes per int
        textBuffer.order(ByteOrder.nativeOrder())
        for (i in textIds.indices) {
            textBuffer.putInt(textIds[i])
        }
        textBuffer.rewind()
        
        // Prepare embedding buffer - use default embedding if not provided
        val embeddingBuffer = ByteBuffer.allocateDirect(EMBEDDING_SIZE * 4) // 4 bytes per float
        embeddingBuffer.order(ByteOrder.nativeOrder())
        if (voiceEmbedding != null) {
            for (value in voiceEmbedding) {
                embeddingBuffer.putFloat(value)
            }
        } else {
            // Use a default/neutral embedding if no custom embedding is provided
            for (i in 0 until EMBEDDING_SIZE) {
                embeddingBuffer.putFloat(0f) // Neutral embedding (all zeros)
            }
        }
        embeddingBuffer.rewind()
        
        // Estimate output audio length - roughly 10 samples per character at 24kHz
        val estimatedOutputLength = cleanedText.length * 10 * 24 // Very rough estimation
        val outputBuffer = ByteBuffer.allocateDirect(estimatedOutputLength * 2) // 16-bit samples
        outputBuffer.order(ByteOrder.nativeOrder())
        
        // Run inference
        synchronized(this) {
            val inputs = mapOf(
                "text_ids" to textBuffer,
                "speaker_embedding" to embeddingBuffer
            )
            val outputs = mapOf(
                "audio" to outputBuffer
            )
            interpreter?.runForMultipleInputsOutputs(inputs, outputs)
        }
        
        // Process output to get audio data
        outputBuffer.rewind()
        
        // In a real application, the model would output the exact number of samples
        // Here we're just taking the first portion of our estimated buffer
        val actualOutputLength = minOf(estimatedOutputLength, outputBuffer.remaining() / 2) * 2
        val audioData = ByteArray(actualOutputLength)
        outputBuffer.get(audioData)
        
        return audioData
    }
    
    /**
     * Clean and normalize text for TTS
     */
    private fun cleanText(text: String): String {
        // Basic text normalization - expand this as needed
        return text.trim()
            .lowercase()
            .replace(Regex("[^a-zA-Z0-9.,!?'\" ]"), " ")
            .replace(Regex("\\s+"), " ")
    }
    
    /**
     * Convert text to model-compatible input IDs
     */
    private fun encodeText(text: String): IntArray {
        val encodedText = IntArray(MAX_TEXT_LENGTH)
        
        // Convert each character to its corresponding ID
        text.take(MAX_TEXT_LENGTH).forEachIndexed { index, char ->
            encodedText[index] = symbolToId[char.toString()] ?: 0
        }
        
        return encodedText
    }
    
    /**
     * Release resources
     */
    fun release() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}
