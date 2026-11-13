package com.studysession.tracker.presentation.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysession.tracker.core.di.AppViewModelFactory
import com.studysession.tracker.core.navigation.Screen
import com.studysession.tracker.presentation.splash.SplashRoute
import com.studysession.tracker.presentation.menu.MenuRoute
import com.studysession.tracker.presentation.game.*
import com.studysession.tracker.presentation.gameover.GameOverRoute
import com.studysession.tracker.presentation.log.SessionLogRoute
import com.studysession.tracker.presentation.chart.WeeklyChartRoute
import com.studysession.tracker.presentation.goals.GoalsRoute
import com.studysession.tracker.presentation.settings.SettingsRoute

@Composable
fun AppRoot() {
    val model: AppViewModel = viewModel(factory = AppViewModelFactory())
    val game: GameViewModel = viewModel(factory = AppViewModelFactory())
    val screen by model.screen.collectAsStateWithLifecycle()
    val gameState by game.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        game.events.collect { event ->
            when (event) {
                is GameEvent.Open -> model.openGame(event.id)
                is GameEvent.Result -> {
                    if (event.haptic) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    model.navigate(Screen.GameOver(event.id))
                }
                GameEvent.Back -> model.back()
            }
        }
    }
    BackHandler(screen != Screen.Splash && screen != Screen.Menu) {
        if (screen is Screen.Game) game.requestExit() else model.back()
    }
    AnimatedContent(targetState = screen,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(240)) }, label = "destination") { destination ->
        when (destination) {
            Screen.Splash -> SplashRoute(model::finishSplash)
            Screen.Menu -> MenuRoute(model::navigate, game::start, game::openExisting, game::saveExisting,
                game::replaceDraft, gameState.creating, gameState.errorRes)
            is Screen.Game -> GameRoute(destination.sessionId, game, model::menu)
            is Screen.GameOver -> GameOverRoute(destination.sessionId, model::back, model::menu, game::start,
                { model.navigate(Screen.SessionLog) }, { model.navigate(Screen.Goals) })
            Screen.SessionLog -> SessionLogRoute(model::back, model::menu)
            Screen.WeeklyChart -> WeeklyChartRoute(model::back, model::menu, { model.navigate(Screen.Goals) })
            Screen.Goals -> GoalsRoute(model::back, model::menu)
            Screen.Settings -> SettingsRoute(model::back)
        }
    }
}
