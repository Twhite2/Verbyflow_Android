package com.example.verbyflow.presentation.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CallScreen(
    viewModel: CallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "VerbyFlow Call",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = { /* Show settings */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Call status
                CallStatusSection(callStatus = uiState.callStatus)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Transcription display
                TranscriptionSection(
                    originalText = uiState.originalTranscription,
                    translatedText = uiState.translatedText
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Call controls
                CallControlsSection(
                    isInCall = uiState.isInCall,
                    isMuted = uiState.isMuted,
                    onStartCall = viewModel::startCall,
                    onEndCall = viewModel::endCall,
                    onToggleMute = viewModel::toggleMute
                )
            }
        }
    }
}

@Composable
fun CallStatusSection(callStatus: String) {
    val statusColor = when(callStatus) {
        "Connected" -> MaterialTheme.colorScheme.primary
        "Connecting..." -> Color(0xFFFFA000) // Amber
        "Disconnected" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = callStatus,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TranscriptionSection(
    originalText: String,
    translatedText: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Original transcription
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Original",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = originalText.ifEmpty { "Waiting for speech..." },
                    fontSize = 16.sp,
                    color = if (originalText.isEmpty()) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Translated text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Translated",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = translatedText.ifEmpty { "Translation will appear here..." },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (translatedText.isEmpty()) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun CallControlsSection(
    isInCall: Boolean,
    isMuted: Boolean,
    onStartCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute button
        if (isInCall) {
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMuted) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardVoice,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Call button
        FloatingActionButton(
            onClick = if (isInCall) onEndCall else onStartCall,
            containerColor = if (isInCall) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                imageVector = if (isInCall) Icons.Default.CallEnd else Icons.Default.Call,
                contentDescription = if (isInCall) "End call" else "Start call",
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
        
        // Placeholder for symmetry when not in call
        if (isInCall) {
            IconButton(
                onClick = { /* Language settings */ },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "EN",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Box(modifier = Modifier.size(56.dp))
        }
    }
}
