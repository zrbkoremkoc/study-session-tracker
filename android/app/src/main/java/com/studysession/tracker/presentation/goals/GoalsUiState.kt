package com.studysession.tracker.presentation.goals

import com.studysession.tracker.domain.model.*

data class GoalsUiState(
    val loadStatus: LoadStatus = LoadStatus.Loading,
    val subjects: List<Subject> = emptyList(),
    val drafts: Map<String, Int> = emptyMap(),
    val saved: Map<String, Int> = emptyMap(),
    val totalMinutes: Int = 0,
    val hasChanges: Boolean = false,
    val saving: Boolean = false,
    val leaveConfirmation: Boolean = false,
    val errorRes: Int? = null
)
