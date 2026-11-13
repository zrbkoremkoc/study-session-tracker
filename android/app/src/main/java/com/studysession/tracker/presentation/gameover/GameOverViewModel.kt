package com.studysession.tracker.presentation.gameover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studysession.tracker.R
import com.studysession.tracker.domain.repository.StudyRepository
import com.studysession.tracker.domain.usecase.WeeklySummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameOverViewModel(private val study: StudyRepository, private val weekly: WeeklySummaryUseCase) : ViewModel() {
    private val mutableState = MutableStateFlow(GameOverUiState())
    val state = mutableState.asStateFlow()
    private var id = ""

    init { viewModelScope.launch { study.state.collect { refresh() } } }
    fun open(sessionId: String) { id = sessionId; refresh() }
    fun retry() { viewModelScope.launch { study.load(); refresh() } }

    private fun refresh() {
        val store = study.state.value
        val session = store.data.sessions.firstOrNull { it.id == id }
        val subject = store.data.subjects.firstOrNull { it.id == session?.subjectId }
        val seconds = session?.let { weekly.calculate(store.data.sessions, weekly.currentMonday()).bySubject[it.subjectId] } ?: 0
        val goal = (subject?.weeklyGoalMinutes ?: 0) * 60L
        mutableState.update { it.copy(loadStatus = store.status, session = session, weekFocusSeconds = seconds,
            goalSeconds = goal, goalProgress = if (goal > 0) (seconds.toFloat() / goal).coerceIn(0f, 1f) else 0f,
            goalReached = goal > 0 && seconds >= goal, canReplay = subject != null && !subject.archived,
            errorRes = if (store.readFailed) R.string.read_error else null) }
    }
}
