# WebRTC Library

If you encounter issues with the Maven repository version of WebRTC, you can download a prebuilt AAR file and place it in this directory.

## Instructions

1. Download a compatible WebRTC AAR file from:
   - https://webrtc.github.io/webrtc-org/native-code/android/
   - https://bintray.com/google/webrtc/google-webrtc
   - https://repo1.maven.org/maven2/org/webrtc/google-webrtc/

2. Rename the downloaded file to `libwebrtc.aar`

3. Place the file in this directory

4. Uncomment the implementation line in build.gradle.kts:
   ```kotlin
   implementation(files("libs/libwebrtc.aar"))
   ```

5. Comment out the Maven implementation:
   ```kotlin
   // implementation(libs.webrtc)
   ```

6. Sync the project in Android Studio
