package com.studysession.tracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studysession.tracker.R
import com.studysession.tracker.domain.model.TimerSettings
import com.studysession.tracker.domain.repository.StudyRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(private val study: StudyRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state = mutableState.asStateFlow()
    private val channel = Channel<Unit>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    init {
        viewModelScope.launch {
            study.state.collect { store ->
                mutableState.update { it.copy(loadStatus = store.status, saved = store.data.settings,
                    draft = if (it.hasChanges) it.draft else store.data.settings,
                    errorRes = if (store.readFailed) R.string.read_error else it.errorRes) }
            }
        }
    }
    fun retry() { viewModelScope.launch { study.load() } }
    fun focus(delta: Int) { edit(state.value.draft.copy(focusMinutes = (state.value.draft.focusMinutes + delta).coerceIn(5, 90))) }
    fun rest(delta: Int) { edit(state.value.draft.copy(breakMinutes = (state.value.draft.breakMinutes + delta).coerceIn(1, 30))) }
    fun defaults() { edit(TimerSettings()) }
    private fun edit(value: TimerSettings) {
        if (!state.value.saving) mutableState.update { it.copy(draft = value, hasChanges = value != it.saved) }
    }
    fun requestBack() {
        if (state.value.saving) return
        if (state.value.hasChanges) mutableState.update { it.copy(leaveConfirmation = true) }
        else viewModelScope.launch { channel.send(Unit) }
    }
    fun keepEditing() { mutableState.update { it.copy(leaveConfirmation = false) } }
    fun discard() {
        mutableState.update { it.copy(draft = it.saved, hasChanges = false, leaveConfirmation = false, errorRes = null) }
        viewModelScope.launch { channel.send(Unit) }
    }
    fun save() {
        if (state.value.saving || !state.value.hasChanges) return
        val draft = state.value.draft
        mutableState.update { it.copy(saving = true, errorRes = null) }
        viewModelScope.launch {
            try {
                study.saveSettings(draft)
                mutableState.update { it.copy(saving = false, hasChanges = false, leaveConfirmation = false, saved = draft) }
                channel.send(Unit)
            } catch (_: Exception) { mutableState.update { it.copy(saving = false, leaveConfirmation = false, errorRes = R.string.write_error) } }
        }
    }
}
