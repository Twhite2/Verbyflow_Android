package com.example.verbyflow.presentation.onboarding

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    
    val context = LocalContext.current
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    LaunchedEffect(uiState.onboardingStep) {
        if (uiState.onboardingStep == OnboardingStep.COMPLETED) {
            delay(1000) // Show completion UI briefly
            onComplete()
        }
    }
    
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = "VerbyFlow",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle
                Text(
                    text = "Real-time voice translation",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Different UI based on onboarding step
                when (uiState.onboardingStep) {
                    OnboardingStep.USER_SETUP -> UserSetupSection(
                        userName = uiState.userName,
                        onUserNameChanged = viewModel::updateUserName,
                        onContinue = viewModel::createUser
                    )
                    
                    OnboardingStep.VOICE_RECORDING -> VoiceRecordingSection(
                        isRecording = isRecording,
                        recordingProgress = uiState.recordingProgress,
                        onStartRecording = {
                            if (micPermissionState.status.isGranted) {
                                viewModel.startRecording()
                            } else {
                                micPermissionState.launchPermissionRequest()
                            }
                        },
                        onStopRecording = viewModel::stopRecording
                    )
                    
                    OnboardingStep.VOICE_PROCESSING -> VoiceProcessingSection()
                    
                    OnboardingStep.COMPLETED -> CompletionSection()
                }
            }
            
            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun UserSetupSection(
    userName: String,
    onUserNameChanged: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Person",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "What's your name?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = userName,
            onValueChange = onUserNameChanged,
            label = { Text("Your name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(50.dp)
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun VoiceRecordingSection(
    isRecording: Boolean,
    recordingProgress: Float,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Voice Setup",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Please record a 5-second sample of your voice",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Voice recording button
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            // Recording progress indicator
            CircularProgressIndicator(
                progress = { recordingProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp
            )
            
            // Mic button
            val buttonScale by animateFloatAsState(
                targetValue = if (isRecording) 0.85f else 1f,
                label = "buttonScale"
            )
            
            FloatingActionButton(
                onClick = {
                    if (isRecording) onStopRecording() else onStartRecording()
                },
                modifier = Modifier.scale(buttonScale),
                shape = CircleShape,
                containerColor = if (isRecording) 
                    MaterialTheme.colorScheme.error
                else 
                    MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardVoice,
                    contentDescription = if (isRecording) "Stop recording" else "Start recording",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isRecording) "Recording... (tap to stop)" else "Tap to start recording",
            fontSize = 16.sp,
            color = if (isRecording) 
                MaterialTheme.colorScheme.error
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        if (isRecording) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Speak naturally for 5 seconds",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun VoiceProcessingSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Processing your voice",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Creating your voice profile...",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun CompletionSection() {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Complete",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "You're all set!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your voice profile has been created",
                fontSize = 16.sp,
                modifier = Modifier.alpha(0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
