package com.studysession.tracker.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.studysession.tracker.domain.usecase.SessionTimerUseCase
import com.studysession.tracker.presentation.app.AppViewModel
import com.studysession.tracker.presentation.splash.SplashViewModel
import com.studysession.tracker.presentation.menu.MenuViewModel
import com.studysession.tracker.presentation.game.GameViewModel
import com.studysession.tracker.presentation.gameover.GameOverViewModel
import com.studysession.tracker.presentation.log.SessionLogViewModel
import com.studysession.tracker.presentation.chart.WeeklyChartViewModel
import com.studysession.tracker.presentation.goals.GoalsViewModel
import com.studysession.tracker.presentation.settings.SettingsViewModel

class AppViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val study = ServiceLocator.study
        val weekly = ServiceLocator.weeklySummary
        val model: ViewModel = when (modelClass) {
            AppViewModel::class.java -> AppViewModel(study)
            SplashViewModel::class.java -> SplashViewModel()
            MenuViewModel::class.java -> MenuViewModel(study, weekly, ServiceLocator.clock)
            GameViewModel::class.java -> GameViewModel(study, SessionTimerUseCase(ServiceLocator.clock))
            GameOverViewModel::class.java -> GameOverViewModel(study, weekly)
            SessionLogViewModel::class.java -> SessionLogViewModel(study)
            WeeklyChartViewModel::class.java -> WeeklyChartViewModel(study, weekly, ServiceLocator.exampleSubjects)
            GoalsViewModel::class.java -> GoalsViewModel(study)
            SettingsViewModel::class.java -> SettingsViewModel(study)
            else -> error(modelClass.name)
        }
        return modelClass.cast(model) ?: error(modelClass.name)
    }
}
