package com.studysession.tracker.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studysession.tracker.R
import com.studysession.tracker.domain.repository.StudyRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GoalsViewModel(private val study: StudyRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(GoalsUiState())
    val state = mutableState.asStateFlow()
    private val channel = Channel<Unit>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    init {
        viewModelScope.launch {
            study.state.collect { store ->
                val subjects = store.data.subjects.filterNot { it.archived }
                val saved = subjects.associate { it.id to it.weeklyGoalMinutes }
                mutableState.update {
                    val drafts = if (it.hasChanges) saved + it.drafts.filterKeys { id -> saved.containsKey(id) } else saved
                    it.copy(loadStatus = store.status, subjects = subjects, saved = saved, drafts = drafts, totalMinutes = drafts.values.sum(),
                        hasChanges = drafts != saved, errorRes = if (store.readFailed) R.string.read_error else it.errorRes)
                }
            }
        }
    }
    fun retry() { viewModelScope.launch { study.load() } }
    fun change(id: String, delta: Int) {
        if (state.value.saving) return
        mutableState.update {
            val drafts = it.drafts + (id to ((it.drafts[id] ?: 0) + delta).coerceIn(0, 2400))
            it.copy(drafts = drafts, totalMinutes = drafts.values.sum(), hasChanges = drafts != it.saved)
        }
    }
    fun requestBack() {
        if (state.value.saving) return
        if (state.value.hasChanges) mutableState.update { it.copy(leaveConfirmation = true) }
        else viewModelScope.launch { channel.send(Unit) }
    }
    fun keepEditing() { mutableState.update { it.copy(leaveConfirmation = false) } }
    fun discard() {
        mutableState.update { it.copy(drafts = it.saved, totalMinutes = it.saved.values.sum(), hasChanges = false, leaveConfirmation = false, errorRes = null) }
        viewModelScope.launch { channel.send(Unit) }
    }
    fun save() {
        if (state.value.saving || !state.value.hasChanges) return
        val drafts = state.value.drafts
        mutableState.update { it.copy(saving = true, errorRes = null) }
        viewModelScope.launch {
            try {
                study.saveGoals(drafts)
                mutableState.update { it.copy(saving = false, hasChanges = false, leaveConfirmation = false) }
                channel.send(Unit)
            } catch (_: Exception) { mutableState.update { it.copy(saving = false, leaveConfirmation = false, errorRes = R.string.write_error) } }
        }
    }
}
