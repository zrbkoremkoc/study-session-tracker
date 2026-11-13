package com.studysession.tracker.presentation.menu

import com.studysession.tracker.domain.model.*

data class SubjectEditorState(
    val id: String? = null,
    val name: String = "",
    val goalMinutes: Int = 60,
    val errorRes: Int? = null,
    val saving: Boolean = false,
    val archiveConfirmation: Boolean = false
)

data class MenuUiState(
    val loadStatus: LoadStatus = LoadStatus.Loading,
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: String? = null,
    val summary: WeekSummary = WeekSummary(),
    val settings: TimerSettings = TimerSettings(),
    val draft: SessionDraft? = null,
    val editor: SubjectEditorState? = null,
    val replaceConfirmation: Boolean = false,
    val errorRes: Int? = null
)
