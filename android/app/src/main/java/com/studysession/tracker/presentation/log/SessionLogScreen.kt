package com.studysession.tracker.presentation.log

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysession.tracker.R
import com.studysession.tracker.core.di.AppViewModelFactory
import com.studysession.tracker.core.theme.*
import com.studysession.tracker.core.ui.*
import com.studysession.tracker.domain.model.*
import com.studysession.tracker.presentation.dialog.ConfirmActionDialog

@Composable
fun SessionLogRoute(onBack: () -> Unit, onStart: () -> Unit) {
    val model: SessionLogViewModel = viewModel(factory = AppViewModelFactory())
    val state by model.state.collectAsStateWithLifecycle()
    BackHandler(state.details != null || state.pendingDeleteId != null) {
        if (state.pendingDeleteId != null) model.cancelDelete() else model.close()
    }
    SessionLogScreen(state, onBack, onStart, model::filter, model::details, model::retry)
    state.details?.let { session ->
        if (state.pendingDeleteId == null) {
            AlertDialog(onDismissRequest = model::close, title = { Text(session.subjectNameSnapshot) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.started_at, dateTimeText(session.startedAt)))
                        Text(stringResource(R.string.ended_at, dateTimeText(session.endedAt)))
                        Text(stringResource(R.string.actual_focus, durationText(session.focusSeconds)))
                        Text(if (session.breakSeconds > 0) stringResource(R.string.break_saved, durationText(session.breakSeconds)) else stringResource(R.string.break_skipped))
                        Text(stringResource(if (session.completionReason == CompletionReason.FocusCompleted) R.string.completed_focus else R.string.finished_early))
                        state.errorRes?.let { Text(stringResource(it)) }
                    }
                }, confirmButton = { SecondaryButton(R.string.close, model::close) },
                dismissButton = { SecondaryButton(R.string.delete_session, model::confirmDelete) })
        } else ConfirmActionDialog(R.string.delete_session, stringResource(R.string.delete_hint, session.subjectNameSnapshot),
            R.string.delete_session, model::delete, model::cancelDelete, state.deleting)
    }
}

@Composable
fun SessionLogScreen(state: SessionLogUiState, onBack: () -> Unit, onStart: () -> Unit, onFilter: (String?) -> Unit,
    onDetails: (String) -> Unit, onRetry: () -> Unit) {
    AppBackground(BackgroundVariant.Journal) {
        ScreenColumn {
            ScreenHeader(stringResource(R.string.session_log_title), onBack)
            Text(stringResource(R.string.log_eyebrow), color = Accent, style = MaterialTheme.typography.labelSmall)
            MutedText(stringResource(R.string.log_hint))
            if (state.loadStatus == LoadStatus.Loading) StatusPanel(R.string.loading_log, loading = true)
            state.errorRes?.let { StatusPanel(it, R.string.retry, onRetry) }
            state.statusRes?.let { Text(stringResource(it), color = Accent, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.selectedFilter == null, { onFilter(null) }, label = { Text(stringResource(R.string.all_subjects)) }, modifier = Modifier.heightIn(min = 48.dp))
                state.filters.forEach { filter ->
                    val semantic = stringResource(if (state.selectedFilter == filter.id) R.string.subject_selected else R.string.subject_not_selected, filter.name)
                    FilterChip(state.selectedFilter == filter.id, { onFilter(filter.id) }, label = { Text(filter.name) },
                        modifier = Modifier.heightIn(min = 48.dp).semantics { stateDescription = semantic })
                }
            }
            if (state.dayGroups.isEmpty() && state.loadStatus != LoadStatus.Loading) {
                if (state.selectedFilter != null) StatusPanel(R.string.no_filtered_sessions, R.string.clear_filter, { onFilter(null) })
                else StatusPanel(R.string.empty_log, R.string.start_session, onStart)
            }
            state.dayGroups.forEach { day ->
                Text(dateText(day.date), color = OnSurface, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                day.sessions.forEach { session ->
                    val detailsDescription = stringResource(R.string.session_details_description, session.subjectNameSnapshot, dateTimeText(session.startedAt))
                    NotebookCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(session.subjectNameSnapshot, color = OnSurface, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Text(durationText(session.focusSeconds), color = Accent, style = MaterialTheme.typography.titleMedium)
                        }
                        MutedText(dateTimeText(session.startedAt))
                        if (session.breakSeconds > 0) MutedText(stringResource(R.string.break_saved, durationText(session.breakSeconds)))
                        TextButton({ onDetails(session.id) }, Modifier.heightIn(min = 48.dp).semantics { contentDescription = detailsDescription }) {
                            Text(stringResource(R.string.view_details))
                        }
                    }
                }
            }
        }
    }
}
