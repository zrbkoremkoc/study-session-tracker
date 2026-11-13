package com.studysession.tracker.presentation.settings

import com.studysession.tracker.domain.model.*

data class SettingsUiState(
    val loadStatus: LoadStatus = LoadStatus.Loading,
    val draft: TimerSettings = TimerSettings(),
    val saved: TimerSettings = TimerSettings(),
    val hasChanges: Boolean = false,
    val saving: Boolean = false,
    val leaveConfirmation: Boolean = false,
    val errorRes: Int? = null
)
