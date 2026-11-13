package com.studysession.tracker.presentation.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studysession.tracker.R
import com.studysession.tracker.domain.repository.StudyRepository
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionLogViewModel(private val study: StudyRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SessionLogUiState())
    val state = mutableState.asStateFlow()
    init { viewModelScope.launch { study.state.collect { refresh() } } }
    fun retry() { viewModelScope.launch { study.load() } }
    fun filter(id: String?) { mutableState.update { it.copy(selectedFilter = id) }; refresh() }
    fun details(id: String) { mutableState.update { it.copy(details = study.state.value.data.sessions.firstOrNull { session -> session.id == id }) } }
    fun close() { mutableState.update { it.copy(details = null, pendingDeleteId = null) } }
    fun confirmDelete() { mutableState.update { it.copy(pendingDeleteId = it.details?.id) } }
    fun cancelDelete() { mutableState.update { it.copy(pendingDeleteId = null) } }
    fun delete() {
        val id = state.value.pendingDeleteId ?: return
        if (state.value.deleting) return
        mutableState.update { it.copy(deleting = true) }
        viewModelScope.launch {
            try { study.deleteSession(id); mutableState.update { it.copy(details = null, pendingDeleteId = null, deleting = false, statusRes = R.string.session_deleted) } }
            catch (_: Exception) { mutableState.update { it.copy(deleting = false, errorRes = R.string.write_error, pendingDeleteId = null) } }
        }
    }
    private fun refresh() {
        val store = study.state.value
        val filtered = store.data.sessions.filter { state.value.selectedFilter == null || it.subjectId == state.value.selectedFilter }.sortedByDescending { it.startedAt }
        val groups = filtered.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate() }
            .map { LogDay(it.key, it.value) }
        val filters = store.data.subjects.filter { subject -> !subject.archived || store.data.sessions.any { it.subjectId == subject.id } }.map { SubjectFilter(it.id, it.name) }
        mutableState.update { it.copy(loadStatus = store.status, dayGroups = groups, filters = filters, errorRes = if (store.readFailed) R.string.read_error else null) }
    }
}
