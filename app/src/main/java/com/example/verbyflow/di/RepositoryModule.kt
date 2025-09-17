package com.example.verbyflow.di

import com.example.verbyflow.data.repository.CallRepositoryImpl
import com.example.verbyflow.data.repository.SpeechRepositoryImpl
import com.example.verbyflow.data.repository.UserRepositoryImpl
import com.example.verbyflow.data.repository.VoiceEmbeddingRepositoryImpl
import com.example.verbyflow.domain.repository.CallRepository
import com.example.verbyflow.domain.repository.SpeechRepository
import com.example.verbyflow.domain.repository.UserRepository
import com.example.verbyflow.domain.repository.VoiceEmbeddingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindVoiceEmbeddingRepository(
        voiceEmbeddingRepositoryImpl: VoiceEmbeddingRepositoryImpl
    ): VoiceEmbeddingRepository

    @Binds
    @Singleton
    abstract fun bindCallRepository(
        callRepositoryImpl: CallRepositoryImpl
    ): CallRepository

    @Binds
    @Singleton
    abstract fun bindSpeechRepository(
        speechRepositoryImpl: SpeechRepositoryImpl
    ): SpeechRepository
}
