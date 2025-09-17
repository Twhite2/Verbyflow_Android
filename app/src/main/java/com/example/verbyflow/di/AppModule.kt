package com.example.verbyflow.di

import android.content.Context
import com.example.verbyflow.data.local.EmbeddingPreferences
import com.example.verbyflow.data.remote.WebRTCClient
import com.example.verbyflow.ml.EcapaTdnnProcessor
import com.example.verbyflow.ml.TTSProcessor
import com.example.verbyflow.ml.TranslationProcessor
import com.example.verbyflow.ml.WhisperProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEmbeddingPreferences(@ApplicationContext context: Context): EmbeddingPreferences {
        return EmbeddingPreferences(context)
    }

    @Provides
    @Singleton
    fun provideWebRTCClient(@ApplicationContext context: Context): WebRTCClient {
        return WebRTCClient(context)
    }

    @Provides
    @Singleton
    fun provideEcapaTdnnProcessor(@ApplicationContext context: Context): EcapaTdnnProcessor {
        return EcapaTdnnProcessor(context)
    }

    @Provides
    @Singleton
    fun provideWhisperProcessor(@ApplicationContext context: Context): WhisperProcessor {
        return WhisperProcessor(context)
    }

    @Provides
    @Singleton
    fun provideTranslationProcessor(@ApplicationContext context: Context): TranslationProcessor {
        return TranslationProcessor(context)
    }

    @Provides
    @Singleton
    fun provideTTSProcessor(@ApplicationContext context: Context): TTSProcessor {
        return TTSProcessor(context)
    }
}
