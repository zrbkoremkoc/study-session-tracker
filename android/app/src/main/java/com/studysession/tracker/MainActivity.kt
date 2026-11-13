package com.studysession.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.lifecycle.ViewModelProvider
import com.studysession.tracker.core.di.AppViewModelFactory
import com.studysession.tracker.core.di.ServiceLocator
import com.studysession.tracker.core.theme.AppTheme
import com.studysession.tracker.presentation.app.AppRoot
import com.studysession.tracker.presentation.game.GameViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        ServiceLocator.initialize(applicationContext)
        setContent { AppTheme { AppRoot() } }
    }

    override fun onStop() {
        ViewModelProvider(this, AppViewModelFactory())[GameViewModel::class.java]
            .foregroundLost(isChangingConfigurations)
        super.onStop()
    }
}
