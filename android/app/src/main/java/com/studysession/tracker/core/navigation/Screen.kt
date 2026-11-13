package com.studysession.tracker.core.navigation

sealed interface Screen {
    data object Splash : Screen
    data object Menu : Screen
    data class Game(val sessionId: String) : Screen
    data class GameOver(val sessionId: String) : Screen
    data object SessionLog : Screen
    data object WeeklyChart : Screen
    data object Goals : Screen
    data object Settings : Screen
}
