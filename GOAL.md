Ah, got it! You want the **pipeline to include WebRTC** for the actual audio streaming (instead of gRPC streaming), while keeping gRPC optional for control/metadata. Here's the updated **Windsurf prompt**:

---

## 📝 **Windsurf Prompt — VerbyFlow Kotlin Development Pipeline (WebRTC + gRPC)**

> **Goal:** Build **VerbyFlow**, a real-time voice translation call app, entirely in **Kotlin** with a **Ktor backend**. The app should support real-time transcription (Whisper), translation, and cloned-voice TTS (DIA), using **WebRTC** for low-latency audio streaming and **gRPC** for optional metadata/control.

---

### **Pipeline Overview**

1. **Onboarding (Voice Embedding Setup)**

   * On first launch, prompt the user to record a **5–10 second audio sample**.
   * Run **ECAPA-TDNN (SpeechBrain)** locally to create a **voice embedding**.
   * Store this embedding **locally** (encrypted) or upload securely to the backend.
   * Subsequent calls reuse this stored embedding for consistent cloned voice.

2. **Call Initialization**

   * Use **WebRTC** to establish a **peer-to-peer audio/video/data channel** between two users.
   * Use **gRPC** only for optional metadata, authentication, or signaling if needed.
   * Ensure audio streaming is low-latency, continuous, and resilient.

3. **Speech-to-Text**

   * Capture microphone input in real time.
   * Send to **Whisper Small** (local or server) for streaming transcription.
   * Pass transcribed text to the translation step.

4. **Translation**

   * Use Whisper’s translation feature **OR** a lightweight local translation model.
   * Output translated text in the target language.

5. **Voice Synthesis**

   * Send translated text + stored **voice embedding** to **DIA TTS**.
   * Receive generated speech in the user’s cloned voice.
   * Play output in near real time to the other participant.

6. **Speaker Verification (Optional)**

   * Periodically re-check active speaker embedding during calls.
   * If the speaker changes, prompt for a new profile or fallback to generic TTS voice.

---

### **Pipeline Flow Diagram**

```
[User Onboarding]
   │
   └─► [5–10s Voice Sample] ─► [ECAPA-TDNN] ─► [Stored Embedding]

[User Mic] ──► [Whisper Streaming: STT + Translation] ──► [DIA TTS + Voice Embedding] ──► Speaker Playback
            │                                         │
            └──────────► WebRTC Peer Streaming ────────┘

[Peer Audio] ──► WebRTC ──► Speaker Playback (translated)
Metadata / Control ──► gRPC ──► Session Coordination, Speaker ID, Optional Fallback
```

---

### **Technology Stack**

**Frontend (Android App - Kotlin)**

* UI: Jetpack Compose
* Audio Capture/Playback: `AudioRecord` / `AudioTrack`
* Networking:

  * **WebRTC**: Real-time audio streaming (P2P)
  * **gRPC**: Optional metadata/control
* Local Models:

  * Whisper Small for STT
  * ECAPA-TDNN for speaker embedding
* TTS Playback: DIA-generated audio streams

**Backend (Ktor Server)**

* Role: Relay for signaling / optional fallback processing
* Endpoints:

  * Voice embedding storage (encrypted)
  * gRPC bi-directional streams for control / metadata
  * Fallback Whisper/Translation endpoint if device is underpowered
* Deployment: Render, Fly.io, Railway, or edge servers

**AI Components**

* STT: Whisper Small (streaming)
* Speaker Embedding: ECAPA-TDNN (pre-call)
* Translation: Whisper translation mode or MarianMT
* Voice Cloning: DIA TTS with speaker embedding

---

### **Expected Latency**

* Target < 400ms extra over a normal VoIP call
* WebRTC ensures near real-time audio streaming
* Precomputed embeddings eliminate lag from re-identification

---

### **MVP Scope**

1. Voice Setup Screen → Record 10s sample → store embedding
2. Call UI → Start Call / End Call buttons
3. Audio Streaming → Capture + WebRTC streaming
4. Real-time Whisper Transcription → Display live captions
5. Translation + TTS → Play translated speech in cloned voice

---

### **Testing Setup**

* Run **two instances** of the app (emulator or devices)
* Use **localhost or LAN IP** for signaling server in development
* Log latency per step to optimize pipeline

---

If you want, I can **also create a task-based roadmap with milestones** specifically for **Kotlin + WebRTC + DIA + Whisper** MVP so you can feed it to Windsurf and break it into sprints.

Do you want me to do that next?
