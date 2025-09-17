package com.example.verbyflow.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.verbyflow.presentation.call.CallScreen
import com.example.verbyflow.presentation.navigation.Screen.*
import com.example.verbyflow.presentation.onboarding.OnboardingScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun Navigation(
    navController: NavHostController,
    startDestination: String,
    navigationViewModel: NavigationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val isOnboarded by navigationViewModel.isUserOnboarded.collectAsState(initial = false)
    
    LaunchedEffect(Unit) {
        navigationViewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateToRoute -> {
                    navController.navigate(event.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = event.saveState
                        }
                        launchSingleTop = event.singleTop
                        restoreState = event.restoreState
                    }
                }
                is NavigationEvent.PopBackStack -> {
                    navController.popBackStack()
                }
                is NavigationEvent.NavigateAndClearBackStack -> {
                    navController.navigate(event.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }
    
    // Use the determined start destination
    NavHost(
        navController = navController, 
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding screen
        composable(route = OnboardingScreen.route) {
            OnboardingScreen(
                onComplete = {
                    navigationViewModel.navigateToHome()
                }
            )
        }
        
        // Home screen
        composable(route = HomeScreen.route) {
            com.example.verbyflow.presentation.home.HomeScreen(
                onStartCall = {
                    navigationViewModel.navigateToCall()
                },
                onProfileSettings = {
                    navigationViewModel.navigateToProfileSettings()
                }
            )
        }
        
        // Call screen
        composable(route = CallScreen.route) {
            CallScreen()
        }
        
        // Profile settings screen
        composable(route = ProfileSettingsScreen.route) {
            com.example.verbyflow.presentation.profile.ProfileSettingsScreen(
                onNavigateBack = {
                    navigationViewModel.popBackStack()
                },
                onVoiceSetupRequest = {
                    navigationViewModel.navigateToOnboarding()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object OnboardingScreen : Screen("onboarding_screen")
    object HomeScreen : Screen("home_screen")
    object CallScreen : Screen("call_screen")
    object ProfileSettingsScreen : Screen("profile_settings_screen")
}
