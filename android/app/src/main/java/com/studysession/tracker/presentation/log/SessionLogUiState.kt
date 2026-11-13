package com.studysession.tracker.presentation.log

import com.studysession.tracker.domain.model.*
import java.time.LocalDate

data class LogDay(val date: LocalDate, val sessions: List<StudySession>)
data class SubjectFilter(val id: String, val name: String)
data class SessionLogUiState(
    val loadStatus: LoadStatus = LoadStatus.Loading,
    val filters: List<SubjectFilter> = emptyList(),
    val selectedFilter: String? = null,
    val dayGroups: List<LogDay> = emptyList(),
    val details: StudySession? = null,
    val pendingDeleteId: String? = null,
    val deleting: Boolean = false,
    val errorRes: Int? = null,
    val statusRes: Int? = null
)
