package com.studysession.tracker.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studysession.tracker.R
import com.studysession.tracker.data.sample.SampleData
import com.studysession.tracker.domain.model.Subject
import com.studysession.tracker.domain.repository.StudyRepository
import com.studysession.tracker.domain.usecase.WeeklySummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeeklyChartViewModel(private val study: StudyRepository, private val weekly: WeeklySummaryUseCase, private val exampleSubjects: List<Subject>) : ViewModel() {
    private val mutableState = MutableStateFlow(WeeklyChartUiState(week = weekly.currentMonday()))
    val state = mutableState.asStateFlow()
    private var personalWeek = weekly.currentMonday()
    init { viewModelScope.launch { study.state.collect { refresh() } } }
    fun retry() { viewModelScope.launch { study.load() } }
    fun previous() {
        if (state.value.showExample) return
        mutableState.update { it.copy(week = it.week.minusDays(7)) }; refresh()
    }
    fun next() {
        if (!state.value.canGoNext || state.value.showExample) return
        mutableState.update { it.copy(week = it.week.plusDays(7)) }; refresh()
    }
    fun select(id: String) { mutableState.update { it.copy(selectedSubjectId = id) } }
    fun example() {
        personalWeek = state.value.week
        mutableState.update { it.copy(showExample = true, week = weekly.currentMonday().minusDays(7), selectedSubjectId = null) }; refresh()
    }
    fun personal() {
        mutableState.update { it.copy(showExample = false, week = personalWeek, selectedSubjectId = null) }; refresh()
    }
    private fun refresh() {
        val store = study.state.value
        val current = state.value
        val subjects = if (current.showExample) exampleSubjects else store.data.subjects
        val sessions = if (current.showExample) SampleData.example(exampleSubjects, current.week) else store.data.sessions
        val summary = weekly.calculate(sessions, current.week)
        val visible = subjects.filter { (summary.bySubject[it.id] ?: 0) > 0 || (!it.archived && it.weeklyGoalMinutes > 0) }
        val largest = visible.maxOfOrNull { maxOf(summary.bySubject[it.id] ?: 0, if (it.archived) 0 else it.weeklyGoalMinutes * 60L) } ?: 1800L
        val scale = ((largest + 1799) / 1800 * 1800).coerceAtLeast(1800)
        val rows = visible.map {
            val focus = summary.bySubject[it.id] ?: 0
            val goal = if (it.archived) 0 else it.weeklyGoalMinutes * 60L
            ChartRow(it, focus, goal, focus.toFloat() / scale, goal.toFloat() / scale, if (goal > 0) (focus * 100 / goal).toInt() else 0)
        }
        mutableState.update { it.copy(loadStatus = store.status, rows = rows, summary = summary, scaleSeconds = scale,
            canGoNext = !it.showExample && it.week.isBefore(weekly.currentMonday()), errorRes = if (store.readFailed) R.string.read_error else null) }
    }
}
