package com.example.verbyflow.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.verbyflow.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _navigationEvents = Channel<NavigationEvent>()
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()
    
    val isUserOnboarded: Flow<Boolean> = userRepository.observeCurrentUser().map { user ->
        user?.isOnboarded ?: false
    }
    
    fun getStartDestination(isOnboarded: Boolean): String {
        return if (isOnboarded) {
            Screen.HomeScreen.route
        } else {
            Screen.OnboardingScreen.route
        }
    }
    
    fun navigateToHome() {
        viewModelScope.launch {
            _navigationEvents.send(
                NavigationEvent.NavigateAndClearBackStack(Screen.HomeScreen.route)
            )
        }
    }
    
    fun navigateToOnboarding() {
        viewModelScope.launch {
            _navigationEvents.send(
                NavigationEvent.NavigateToRoute(Screen.OnboardingScreen.route)
            )
        }
    }
    
    fun navigateToCall() {
        viewModelScope.launch {
            _navigationEvents.send(
                NavigationEvent.NavigateToRoute(Screen.CallScreen.route)
            )
        }
    }
    
    fun navigateToProfileSettings() {
        viewModelScope.launch {
            _navigationEvents.send(
                NavigationEvent.NavigateToRoute(Screen.ProfileSettingsScreen.route)
            )
        }
    }
    
    fun popBackStack() {
        viewModelScope.launch {
            _navigationEvents.send(NavigationEvent.PopBackStack)
        }
    }
}

sealed class NavigationEvent {
    data class NavigateToRoute(
        val route: String,
        val singleTop: Boolean = true,
        val saveState: Boolean = true,
        val restoreState: Boolean = true
    ) : NavigationEvent()
    
    object PopBackStack : NavigationEvent()
    
    data class NavigateAndClearBackStack(val route: String) : NavigationEvent()
}
