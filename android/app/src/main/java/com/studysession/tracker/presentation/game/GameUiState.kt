package com.studysession.tracker.presentation.game

import com.studysession.tracker.domain.model.*

data class GameUiState(
    val draft: SessionDraft? = null,
    val remainingSeconds: Long = 0,
    val progress: Float = 0f,
    val isPaused: Boolean = true,
    val creating: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.Idle,
    val exitConfirmation: Boolean = false,
    val restored: Boolean = false,
    val completed: Boolean = false,
    val errorRes: Int? = null
)

sealed interface GameEvent {
    data class Open(val id: String) : GameEvent
    data class Result(val id: String, val haptic: Boolean = false) : GameEvent
    data object Back : GameEvent
}
