package com.studysession.tracker.presentation.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysession.tracker.R
import com.studysession.tracker.core.di.AppViewModelFactory
import com.studysession.tracker.core.theme.*
import com.studysession.tracker.core.ui.*
import com.studysession.tracker.domain.model.LoadStatus
import com.studysession.tracker.presentation.dialog.UnsavedChangesDialog

@Composable
fun GoalsRoute(onBack: () -> Unit, onSubjects: () -> Unit) {
    val model: GoalsViewModel = viewModel(factory = AppViewModelFactory())
    val state by model.state.collectAsStateWithLifecycle()
    val back by rememberUpdatedState(onBack)
    LaunchedEffect(Unit) { model.events.collect { back() } }
    BackHandler { if (state.leaveConfirmation) model.keepEditing() else model.requestBack() }
    GoalsScreen(state, model::requestBack, onSubjects, model::change, model::save, model::retry)
    if (state.leaveConfirmation) UnsavedChangesDialog(model::save, model::discard, model::keepEditing, state.saving)
}

@Composable
fun GoalsScreen(state: GoalsUiState, onBack: () -> Unit, onSubjects: () -> Unit, onChange: (String, Int) -> Unit, onSave: () -> Unit, onRetry: () -> Unit) {
    AppBackground(BackgroundVariant.Plan) {
        ScreenColumn {
            ScreenHeader(stringResource(R.string.weekly_goals_title), onBack)
            Heading(stringResource(R.string.plan_balanced_week))
            MutedText(stringResource(R.string.goals_intro))
            if (state.loadStatus == LoadStatus.Loading) StatusPanel(R.string.loading_goals, loading = true)
            state.errorRes?.let { StatusPanel(it, R.string.retry, onRetry) }
            NotebookCard(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(stringResource(R.string.total_plan), color = Accent, style = MaterialTheme.typography.labelSmall)
                Text(goalText(state.totalMinutes * 60L), color = OnSurface, style = MaterialTheme.typography.displaySmall)
            }
            if (state.subjects.isEmpty() && state.loadStatus != LoadStatus.Loading) StatusPanel(R.string.no_goal_subjects, R.string.subjects, onSubjects)
            state.subjects.forEach { subject ->
                val minutes = state.drafts[subject.id] ?: 0
                NotebookCard {
                    DurationStepper(subject.name, goalText(minutes * 60L), { onChange(subject.id, -30) }, { onChange(subject.id, 30) },
                        minutes > 0 && !state.saving, minutes < 2400 && !state.saving)
                }
            }
            MutedText(stringResource(R.string.goals_hint))
            MutedText(stringResource(R.string.goals_apply))
            PrimaryButton(if (state.errorRes == null) R.string.save_goals else R.string.retry_save, onSave,
                enabled = state.hasChanges && !state.saving, loading = state.saving)
        }
    }
}
