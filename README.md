# VerbyFlow

VerbyFlow is a real-time voice translation call app built entirely in Kotlin. It uses WebRTC for low-latency audio streaming, along with on-device ML models for speech recognition, translation, and voice cloning.

## 📱 Features

- **Voice Profile Creation**: Record a 5-10 second voice sample to create your voice embedding
- **Real-time Voice Translation**: Speak in your language, and have your voice translated in real-time
- **Voice Cloning**: The translated speech is delivered in your own voice
- **WebRTC Communication**: Low-latency peer-to-peer audio streaming
- **Multiple Languages**: Support for translating between multiple languages

## 🏗️ Architecture

VerbyFlow follows a clean architecture pattern with the following layers:

### Domain Layer
- **Models**: Data classes that represent entities in the application
- **Repositories**: Interfaces defining the data operations
- **Use Cases**: Business logic components that orchestrate the flow of data

### Data Layer
- **Repository Implementations**: Concrete implementations of the domain repositories
- **Local Data Sources**: DataStore for user preferences and voice embeddings
- **Remote Data Sources**: WebRTC and gRPC clients

### Presentation Layer
- **ViewModels**: Manage UI state and business logic for each screen
- **Composable UIs**: Jetpack Compose UIs for various screens
- **Navigation**: Navigation component to manage screen transitions

### ML Components
- **EcapaTdnnProcessor**: Voice embedding generation
- **WhisperProcessor**: Speech-to-text transcription
- **TranslationProcessor**: Text translation
- **TTSProcessor**: Text-to-speech with voice cloning

## 🔄 Data Flow

1. **Onboarding Flow**:
   - User records a voice sample
   - ECAPA-TDNN generates a voice embedding
   - Embedding is stored locally

2. **Call Flow**:
   - WebRTC establishes a peer connection
   - Audio is streamed in real-time
   - Whisper transcribes the audio
   - Translation model converts the text
   - DIA TTS synthesizes speech in the user's voice

## 🛠️ Tech Stack

- **UI**: Jetpack Compose
- **Navigation**: Jetpack Navigation Compose
- **Dependency Injection**: Dagger Hilt
- **Audio Processing**: WebRTC, AudioRecord/AudioTrack
- **ML Models**: TensorFlow Lite
- **Storage**: DataStore
- **Networking**: WebRTC, gRPC
- **Asynchronous Programming**: Kotlin Coroutines, Flow

## 📋 Project Structure

- **app/src/main/java/com/example/verbyflow/**
  - **data/**: Repository implementations, local and remote data sources
  - **di/**: Dependency injection modules
  - **domain/**: Models, repository interfaces, and use cases
  - **ml/**: Machine learning processors for voice, speech, and translation
  - **presentation/**: UI components organized by feature
  - **utils/**: Utility classes and extensions

## 🚀 Getting Started

1. Clone the repository
2. Open the project in Android Studio
3. Make sure you have the required ML model files in the assets folder:
   - `ecapa_tdnn_embedding.tflite`
   - `whisper_small.tflite`
   - `marian_mt.tflite`
   - `dia_tts.tflite`
4. Build and run the app on an emulator or physical device

## 📱 Screens

1. **Onboarding Screen**: Initial setup and voice profile creation
2. **Home Screen**: Main hub for starting calls and accessing settings
3. **Call Screen**: The interface for making translated calls
4. **Profile Settings Screen**: Configure user profile and language preferences

## 🔮 Future Enhancements

- Implement user authentication
- Add support for video calls
- Improve translation accuracy with better models
- Add more languages
- Optimize ML models for better performance
- Create a backend for user discovery and signaling

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.
