package com.studysession.tracker.presentation.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysession.tracker.R
import com.studysession.tracker.core.di.AppViewModelFactory
import com.studysession.tracker.core.navigation.Screen
import com.studysession.tracker.core.theme.*
import com.studysession.tracker.core.ui.*
import com.studysession.tracker.domain.model.*
import com.studysession.tracker.presentation.dialog.ConfirmActionDialog
import java.time.LocalDate

@Composable
fun MenuRoute(onNavigate: (Screen) -> Unit, onStart: (String) -> Unit, onResume: () -> Unit,
    onSaveDraft: () -> Unit, onReplace: (String) -> Unit, starting: Boolean, startError: Int?) {
    val model: MenuViewModel = viewModel(factory = AppViewModelFactory())
    val state by model.state.collectAsStateWithLifecycle()
    BackHandler(state.editor != null || state.replaceConfirmation) {
        if (state.replaceConfirmation) model.replaceConfirmation(false) else model.closeEditor()
    }
    MenuScreen(state, onNavigate, onStart, onResume, { model.replaceConfirmation(true) }, model::select, model::edit, model::retry, starting, startError)
    state.editor?.let { editor ->
        SubjectEditorSheet(editor, model::name, model::goal, model::saveSubject, { model.archiveConfirmation(true) }, model::closeEditor)
        if (editor.archiveConfirmation) ConfirmActionDialog(R.string.archive_title, stringResource(R.string.archive_hint, editor.name), R.string.archive,
            model::archive, { model.archiveConfirmation(false) }, editor.saving)
    }
    if (state.replaceConfirmation) {
        AlertDialog(onDismissRequest = { model.replaceConfirmation(false) }, title = { Text(stringResource(R.string.existing_session)) },
            text = { Text(stringResource(R.string.replace_hint)) },
            confirmButton = {
                SecondaryButton(R.string.save_and_finish, { model.replaceConfirmation(false); onSaveDraft() }, enabled = (state.draft?.focusSeconds ?: 0) > 0)
            }, dismissButton = {
                Column {
                    SecondaryButton(R.string.discard, { model.replaceConfirmation(false); state.selectedSubjectId?.let(onReplace) })
                    TextButton({ model.replaceConfirmation(false) }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
                }
            })
    }
}

@Composable
fun MenuScreen(state: MenuUiState, onNavigate: (Screen) -> Unit, onStart: (String) -> Unit, onResume: () -> Unit,
    onNew: () -> Unit, onSelect: (String) -> Unit, onEdit: (Subject?) -> Unit, onRetry: () -> Unit, starting: Boolean, startError: Int?) {
    val sprites = listOf(R.drawable.sprite_1, R.drawable.sprite_2, R.drawable.sprite_3, R.drawable.sprite_4, R.drawable.sprite_5)
    AppBackground(BackgroundVariant.Desk) {
        ScreenColumn(footer = { SessionStartCard(state, onStart, onResume, onNew, starting) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.desk_eyebrow), color = Accent, style = MaterialTheme.typography.labelSmall)
                    Heading(stringResource(R.string.your_desk))
                }
                TextButton({ onNavigate(Screen.Settings) }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.settings)) }
            }
            NotebookCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.hero_title), color = OnSurface, style = MaterialTheme.typography.titleLarge)
                        MutedText(dateText(LocalDate.now()))
                    }
                    Image(painterResource(R.drawable.logo), null, Modifier.size(88.dp), contentScale = ContentScale.Fit)
                }
            }
            if (state.loadStatus == LoadStatus.Loading) StatusPanel(R.string.loading_desk, loading = true)
            state.errorRes?.let { StatusPanel(it, R.string.retry, onRetry) }
            startError?.let { StatusPanel(it) }
            if (state.summary.totalSeconds > 0 && state.summary.activeDays > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(R.string.study_this_week, durationText(state.summary.totalSeconds), Modifier.weight(1f))
                    StatCard(R.string.active_days, state.summary.activeDays.toString(), Modifier.weight(1f))
                }
            } else MutedText(stringResource(R.string.first_session))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.subjects_title), style = MaterialTheme.typography.titleLarge, color = OnSurface, modifier = Modifier.semantics { heading() })
                SecondaryButton(R.string.add_subject, { onEdit(null) }, enabled = state.subjects.size < 12 && state.loadStatus == LoadStatus.Ready)
            }
            MutedText(stringResource(if (state.subjects.size >= 12) R.string.subject_limit else R.string.starter_hint))
            if (state.subjects.isEmpty() && state.loadStatus != LoadStatus.Loading) StatusPanel(R.string.no_subjects)
            BoxWithConstraints {
                val columns = if (maxWidth >= 560.dp) 2 else 1
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.subjects.chunked(columns).forEachIndexed { group, subjects ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            subjects.forEachIndexed { index, subject ->
                                SubjectCard(subject, state.summary.bySubject[subject.id] ?: 0, subject.id == state.selectedSubjectId,
                                    sprites[(group * columns + index) % sprites.size], { onSelect(subject.id) }, { onEdit(subject) }, Modifier.weight(1f))
                            }
                            if (subjects.size < columns) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(R.string.session_log, { onNavigate(Screen.SessionLog) }, Modifier.weight(1f))
                SecondaryButton(R.string.weekly_chart, { onNavigate(Screen.WeeklyChart) }, Modifier.weight(1f))
            }
            SecondaryButton(R.string.goals, { onNavigate(Screen.Goals) }, Modifier.fillMaxWidth())
            MutedText(stringResource(R.string.offline_footer), Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SessionStartCard(state: MenuUiState, onStart: (String) -> Unit, onResume: () -> Unit, onNew: () -> Unit, starting: Boolean) {
    val selected = state.subjects.firstOrNull { it.id == state.selectedSubjectId }
    NotebookCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.sprite_5), null, Modifier.size(36.dp), contentScale = ContentScale.Fit)
            Text(selected?.name ?: stringResource(R.string.choose_subject), style = MaterialTheme.typography.titleLarge, color = OnSurface)
        }
        MutedText(stringResource(R.string.timer_plan, state.settings.focusMinutes, state.settings.breakMinutes))
        if (state.draft != null) {
            Text(stringResource(R.string.paused_draft, state.draft.subjectName, durationText(state.draft.focusSeconds)), color = OnSurface)
            PrimaryButton(R.string.resume_session, onResume, enabled = !starting)
            SecondaryButton(R.string.new_session, onNew, Modifier.fillMaxWidth(), enabled = selected != null && !starting)
        } else {
            val description = stringResource(R.string.start_subject, selected?.name ?: stringResource(R.string.choose_subject))
            PrimaryButton(R.string.play, { selected?.id?.let(onStart) }, Modifier.semantics { contentDescription = description },
                enabled = selected != null && state.loadStatus == LoadStatus.Ready && !starting, loading = starting)
        }
    }
}

@Composable
private fun SubjectCard(subject: Subject, seconds: Long, selected: Boolean, sprite: Int, onSelect: () -> Unit, onEdit: () -> Unit, modifier: Modifier) {
    val semantic = stringResource(if (selected) R.string.subject_selected else R.string.subject_not_selected, subject.name)
    val editDescription = stringResource(R.string.edit_subject_description, subject.name)
    NotebookCard(modifier.semantics { stateDescription = semantic }, selected) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(sprite), null, Modifier.size(46.dp), contentScale = ContentScale.Fit)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                MutedText(if (seconds > 0) stringResource(R.string.subject_week_time, durationText(seconds)) else stringResource(R.string.no_sessions_yet))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onSelect, Modifier.heightIn(min = 48.dp), border = BorderStroke(1.dp, if (selected) Accent else OnSurface.copy(alpha = 0.18f))) {
                Text(stringResource(if (selected) R.string.selected else R.string.select), color = if (selected) Accent else OnSurface)
            }
            TextButton(onEdit, Modifier.heightIn(min = 48.dp).semantics { contentDescription = editDescription }) { Text(stringResource(R.string.edit)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectEditorSheet(state: SubjectEditorState, onName: (String) -> Unit, onGoal: (Int) -> Unit, onSave: () -> Unit, onArchive: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Background) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Heading(stringResource(if (state.id == null) R.string.add_subject_title else R.string.edit_subject_title))
            OutlinedTextField(state.name, onName, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.subject_name)) }, singleLine = true,
                enabled = !state.saving, isError = state.errorRes != null)
            DurationStepper(stringResource(R.string.weekly_goal), goalText(state.goalMinutes * 60L), { onGoal(-30) }, { onGoal(30) },
                state.goalMinutes > 0 && !state.saving, state.goalMinutes < 2400 && !state.saving)
            state.errorRes?.let { Text(stringResource(it), color = OnSurface, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            PrimaryButton(R.string.save, onSave, enabled = !state.saving, loading = state.saving)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(R.string.cancel, onDismiss, Modifier.weight(1f), enabled = !state.saving)
                if (state.id != null) SecondaryButton(R.string.archive, onArchive, Modifier.weight(1f), enabled = !state.saving)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
