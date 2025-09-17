package com.example.verbyflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.verbyflow.presentation.navigation.Navigation
import com.example.verbyflow.presentation.navigation.NavigationViewModel
import com.example.verbyflow.presentation.navigation.Screen
import com.example.verbyflow.ui.theme.VerbyflowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VerbyflowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navigationViewModel: NavigationViewModel = hiltViewModel()
                    
                    val isOnboarded by navigationViewModel.isUserOnboarded.collectAsState(initial = false)
                    val snackbarHostState = remember { SnackbarHostState() }
                    
                    // Determine start destination based on onboarding status
                    val startDestination = navigationViewModel.getStartDestination(isOnboarded)
                    
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Navigation(
                            navController = navController,
                            startDestination = startDestination,
                            navigationViewModel = navigationViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}