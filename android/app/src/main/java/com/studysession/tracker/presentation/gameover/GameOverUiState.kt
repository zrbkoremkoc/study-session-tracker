package com.studysession.tracker.presentation.gameover

import com.studysession.tracker.domain.model.*

data class GameOverUiState(
    val loadStatus: LoadStatus = LoadStatus.Loading,
    val session: StudySession? = null,
    val weekFocusSeconds: Long = 0,
    val goalSeconds: Long = 0,
    val goalProgress: Float = 0f,
    val goalReached: Boolean = false,
    val canReplay: Boolean = false,
    val errorRes: Int? = null
)
