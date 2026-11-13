package com.studysession.tracker.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studysession.tracker.core.navigation.Screen
import com.studysession.tracker.domain.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(val backStack: List<Screen> = emptyList())

class AppViewModel(private val study: StudyRepository) : ViewModel() {
    private val mutableScreen = MutableStateFlow<Screen>(Screen.Splash)
    val screen = mutableScreen.asStateFlow()
    private val mutableState = MutableStateFlow(AppUiState())
    val state = mutableState.asStateFlow()

    init { viewModelScope.launch { study.load() } }

    fun finishSplash() {
        if (screen.value == Screen.Splash) mutableScreen.value = Screen.Menu
    }

    fun navigate(next: Screen) {
        if (next == screen.value || screen.value == Screen.Splash) return
        mutableState.update { it.copy(backStack = it.backStack + screen.value) }
        mutableScreen.value = next
    }

    fun openGame(id: String) {
        mutableState.update { it.copy(backStack = listOf(Screen.Menu)) }
        mutableScreen.value = Screen.Game(id)
    }

    fun menu() {
        mutableState.update { it.copy(backStack = emptyList()) }
        mutableScreen.value = Screen.Menu
    }

    fun back() {
        val stack = state.value.backStack
        mutableScreen.value = stack.lastOrNull() ?: Screen.Menu
        mutableState.update { it.copy(backStack = stack.dropLast(1)) }
    }
}
