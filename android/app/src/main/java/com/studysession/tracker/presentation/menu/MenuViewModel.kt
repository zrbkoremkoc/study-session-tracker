package com.studysession.tracker.presentation.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studysession.tracker.R
import com.studysession.tracker.domain.model.*
import com.studysession.tracker.domain.repository.StudyRepository
import com.studysession.tracker.domain.usecase.WeeklySummaryUseCase
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MenuViewModel(private val study: StudyRepository, private val weekly: WeeklySummaryUseCase, private val clock: StudyClock) : ViewModel() {
    private val mutableState = MutableStateFlow(MenuUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            study.state.collect { store ->
                mutableState.update { it.copy(loadStatus = store.status, subjects = store.data.subjects.filterNot { subject -> subject.archived },
                    selectedSubjectId = store.data.selectedSubjectId, settings = store.data.settings, draft = store.data.activeDraft,
                    summary = weekly.calculate(store.data.sessions, weekly.currentMonday()),
                    errorRes = if (store.readFailed) R.string.read_error else null) }
            }
        }
    }

    fun retry() { viewModelScope.launch { study.load() } }
    fun select(id: String) { viewModelScope.launch { try { study.selectSubject(id) } catch (_: Exception) { error() } } }
    fun edit(subject: Subject?) {
        mutableState.update { it.copy(editor = SubjectEditorState(subject?.id, subject?.name ?: "", subject?.weeklyGoalMinutes ?: 60)) }
    }
    fun name(value: String) { mutableState.update { it.copy(editor = it.editor?.copy(name = value, errorRes = null)) } }
    fun goal(delta: Int) { mutableState.update { it.copy(editor = it.editor?.let { editor -> editor.copy(goalMinutes = (editor.goalMinutes + delta).coerceIn(0, 2400)) }) } }
    fun closeEditor() { if (state.value.editor?.saving != true) mutableState.update { it.copy(editor = null) } }
    fun archiveConfirmation(value: Boolean) { mutableState.update { it.copy(editor = it.editor?.copy(archiveConfirmation = value)) } }
    fun replaceConfirmation(value: Boolean) { mutableState.update { it.copy(replaceConfirmation = value) } }

    fun saveSubject() {
        val editor = state.value.editor ?: return
        if (editor.saving) return
        val name = editor.name.trim()
        val invalid = when {
            name.codePointCount(0, name.length) !in 1..32 -> R.string.name_validation
            state.value.subjects.any { it.id != editor.id && it.name.equals(name, true) } -> R.string.duplicate_subject
            editor.id == null && state.value.subjects.size >= 12 -> R.string.subject_limit
            else -> null
        }
        if (invalid != null) { mutableState.update { it.copy(editor = editor.copy(errorRes = invalid)) }; return }
        mutableState.update { it.copy(editor = editor.copy(saving = true)) }
        viewModelScope.launch {
            try {
                val old = study.state.value.data.subjects.firstOrNull { it.id == editor.id }
                study.saveSubject(Subject(editor.id ?: UUID.randomUUID().toString(), name, editor.goalMinutes, old?.createdAt ?: clock.wallMillis()))
                mutableState.update { it.copy(editor = null) }
            } catch (_: Exception) { mutableState.update { it.copy(editor = editor.copy(errorRes = R.string.write_error)) } }
        }
    }

    fun archive() {
        val editor = state.value.editor ?: return
        val id = editor.id ?: return
        mutableState.update { it.copy(editor = editor.copy(saving = true)) }
        viewModelScope.launch {
            try { study.archiveSubject(id); mutableState.update { it.copy(editor = null) } }
            catch (_: Exception) { mutableState.update { it.copy(editor = editor.copy(errorRes = R.string.write_error, archiveConfirmation = false)) } }
        }
    }

    private fun error() { mutableState.update { it.copy(errorRes = R.string.write_error) } }
}
