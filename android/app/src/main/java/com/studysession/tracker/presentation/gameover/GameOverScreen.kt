package com.studysession.tracker.presentation.gameover

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysession.tracker.R
import com.studysession.tracker.core.di.AppViewModelFactory
import com.studysession.tracker.core.theme.*
import com.studysession.tracker.core.ui.*
import com.studysession.tracker.domain.model.*

@Composable
fun GameOverRoute(sessionId: String, onBack: () -> Unit, onMenu: () -> Unit, onReplay: (String) -> Unit, onLog: () -> Unit, onGoals: () -> Unit) {
    val model: GameOverViewModel = viewModel(factory = AppViewModelFactory())
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(sessionId) { model.open(sessionId) }
    GameOverScreen(state, onBack, onMenu, onReplay, onLog, onGoals, model::retry)
}

@Composable
fun GameOverScreen(state: GameOverUiState, onBack: () -> Unit, onMenu: () -> Unit, onReplay: (String) -> Unit,
    onLog: () -> Unit, onGoals: () -> Unit, onRetry: () -> Unit) {
    val session = state.session
    AppBackground(BackgroundVariant.Result) {
        ScreenColumn(footer = {
            if (session != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MutedText(stringResource(if (state.canReplay) R.string.keep_rhythm else R.string.choose_active_subject))
                    PrimaryButton(R.string.play_again, { if (state.canReplay) onReplay(session.subjectId) else onMenu() })
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecondaryButton(R.string.menu, onMenu, Modifier.weight(1f))
                        SecondaryButton(R.string.view_log, onLog, Modifier.weight(1f))
                    }
                }
            }
        }) {
            ScreenHeader(stringResource(R.string.session_saved), onBack)
            if (state.loadStatus == LoadStatus.Loading) StatusPanel(R.string.loading_result, loading = true)
            state.errorRes?.let { StatusPanel(it, R.string.retry, onRetry) }
            if (session == null && state.loadStatus != LoadStatus.Loading) {
                StatusPanel(R.string.missing_session, R.string.menu, onMenu)
                SecondaryButton(R.string.view_log, onLog, Modifier.fillMaxWidth())
            } else if (session != null) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(148.dp), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.sprite_5), null, Modifier.size(128.dp), contentScale = ContentScale.Fit)
                        SaveCheckmark(Modifier.align(Alignment.BottomEnd))
                    }
                    Heading(stringResource(R.string.time_well_spent))
                    Text(session.subjectNameSnapshot, color = OnSurface.copy(alpha = 0.76f), style = MaterialTheme.typography.titleLarge)
                }
                NotebookCard(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                    Text(stringResource(R.string.focus_saved_label), color = Accent, style = MaterialTheme.typography.labelSmall)
                    Text(durationText(session.focusSeconds), color = OnSurface, style = MaterialTheme.typography.displaySmall)
                    MutedText(stringResource(if (session.completionReason == CompletionReason.FinishedEarly) R.string.finished_early else R.string.completed_focus))
                    MutedText(if (session.breakSeconds > 0) stringResource(R.string.break_saved, durationText(session.breakSeconds)) else stringResource(R.string.break_skipped))
                    MutedText(dateTimeText(session.endedAt))
                }
                if (state.goalSeconds > 0) GoalProgressCard(session.subjectNameSnapshot, durationText(state.weekFocusSeconds), durationText(state.goalSeconds), state.goalProgress, state.goalReached)
                else SecondaryButton(R.string.set_goal, onGoals, Modifier.fillMaxWidth())
            }
        }
    }
}
