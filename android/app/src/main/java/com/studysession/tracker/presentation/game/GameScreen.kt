package com.studysession.tracker.presentation.game

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studysession.tracker.R
import com.studysession.tracker.core.theme.*
import com.studysession.tracker.core.ui.*
import com.studysession.tracker.domain.model.*

@Composable
fun GameRoute(sessionId: String, model: GameViewModel, onMenu: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sessionId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && model.state.value.draft?.id == sessionId) model.attach()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (model.state.value.draft?.id == sessionId) model.attach()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (model.state.value.draft?.id == sessionId) model.pause(activity?.isChangingConfigurations == true)
        }
    }
    GameScreen(state, model::requestExit, model::togglePause, model::startBreak, model::save, model::keepPaused, model::viewResult, onMenu)
    if (state.exitConfirmation) {
        AlertDialog(onDismissRequest = { model.dismissExit() }, title = { Text(stringResource(R.string.end_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if ((state.draft?.focusSeconds ?: 0) > 0) stringResource(R.string.save_focus_hint, durationText(state.draft?.focusSeconds ?: 0)) else stringResource(R.string.no_study_time))
                    MutedText(stringResource(R.string.finish_hint))
                }
            }, confirmButton = { SecondaryButton(R.string.save_and_finish, model::save, enabled = (state.draft?.focusSeconds ?: 0) > 0) },
            dismissButton = {
                Column {
                    SecondaryButton(R.string.resume, { model.dismissExit(true) })
                    TextButton({ model.dismissExit() }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.keep_studying)) }
                    TextButton(model::discard, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.discard)) }
                }
            })
    }
}

@Composable
fun GameScreen(state: GameUiState, onBack: () -> Unit, onToggle: () -> Unit, onBreak: () -> Unit, onSave: () -> Unit,
    onKeepPaused: () -> Unit, onResult: () -> Unit, onMenu: () -> Unit) {
    val draft = state.draft
    AppBackground(BackgroundVariant.Timer) {
        ScreenColumn(footer = { if (draft != null) TimerControls(state, onBack, onToggle, onBreak, onSave, onKeepPaused, onResult, onMenu) }) {
            ScreenHeader(draft?.subjectName ?: stringResource(R.string.session_timer), onBack)
            if (draft == null) {
                StatusPanel(R.string.no_active_session, R.string.menu, onMenu)
            } else {
                Text(stringResource(if (draft.phase == Phase.Break) R.string.break_eyebrow else R.string.focus_eyebrow),
                    color = Accent, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                NotebookCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(if (draft.phase == Phase.Break) R.string.short_break else R.string.focus_time), style = MaterialTheme.typography.titleLarge, color = OnSurface)
                        TimerDial(state)
                        if (state.completed) Text(stringResource(R.string.actual_focus, durationText(draft.focusSeconds)), color = Accent)
                        else MutedText(stringResource(R.string.planned_duration, durationText(if (draft.phase == Phase.Break) draft.plannedBreakSeconds else draft.plannedFocusSeconds)))
                        MutedText(stringResource(R.string.only_focus))
                    }
                }
                Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Image(painterResource(R.drawable.sprite_1), null, Modifier.size(44.dp), contentScale = ContentScale.Fit)
                    Image(painterResource(R.drawable.sprite_3), null, Modifier.size(44.dp), contentScale = ContentScale.Fit)
                }
                MutedText(stringResource(R.string.timer_footer), Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun TimerControls(state: GameUiState, onBack: () -> Unit, onToggle: () -> Unit, onBreak: () -> Unit, onSave: () -> Unit,
    onKeepPaused: () -> Unit, onResult: () -> Unit, onMenu: () -> Unit) {
    val draft = state.draft ?: return
    NotebookCard {
        val status = when {
            state.saveStatus == SaveStatus.Saving -> R.string.saving_session
            state.completed -> R.string.session_saved
            state.restored -> R.string.session_restored
            draft.phase == Phase.BreakReady -> R.string.focus_complete
            state.isPaused -> R.string.timer_paused
            draft.phase == Phase.Break -> R.string.break_running
            else -> R.string.focus_running
        }
        Text(stringResource(status), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = OnSurface, style = MaterialTheme.typography.bodyLarge)
        state.errorRes?.let { Text(stringResource(it), color = OnSurface, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        when {
            state.saveStatus == SaveStatus.Saving -> PrimaryButton(R.string.saving, onSave, enabled = false, loading = true)
            state.completed -> {
                SaveCheckmark(Modifier.align(Alignment.CenterHorizontally))
                PrimaryButton(R.string.view_result, onResult)
                SecondaryButton(R.string.menu, onMenu, Modifier.fillMaxWidth())
            }
            state.saveStatus == SaveStatus.Failed -> {
                PrimaryButton(R.string.retry_save, onSave)
                SecondaryButton(R.string.keep_paused, onKeepPaused, Modifier.fillMaxWidth())
            }
            draft.phase == Phase.BreakReady -> {
                PrimaryButton(R.string.start_break, onBreak)
                SecondaryButton(R.string.finish_session, onSave, Modifier.fillMaxWidth())
            }
            draft.phase == Phase.Completed -> PrimaryButton(R.string.finish_session, onSave)
            else -> {
                Crossfade(state.isPaused, animationSpec = tween(150), label = "pause control") { paused ->
                    PrimaryButton(if (paused) R.string.resume else R.string.pause, onToggle,
                        iconRes = if (!paused) R.drawable.icon_pause else null)
                }
                SecondaryButton(R.string.end_session, onBack, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TimerDial(state: GameUiState) {
    val phase = stringResource(if (state.draft?.phase == Phase.Break) R.string.short_break else R.string.focus_time)
    val running = stringResource(if (state.isPaused) R.string.paused else R.string.running)
    val shownSeconds = if (state.completed) state.draft?.focusSeconds ?: 0 else state.remainingSeconds
    val semantic = if (state.completed) stringResource(R.string.saved_focus_state, durationText(shownSeconds))
        else stringResource(R.string.timer_state, phase, shownSeconds / 60, shownSeconds % 60, running)
    val progress by animateFloatAsState(state.progress, tween(180), label = "elapsed focus")
    val color = if (state.draft?.phase == Phase.Break) Primary else Accent
    Box(Modifier.size(216.dp).semantics { stateDescription = semantic }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = 10.dp.toPx()
            drawArc(OnSurface.copy(alpha = 0.10f), -90f, 360f, false, Offset(stroke / 2, stroke / 2),
                Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color.copy(alpha = if (state.isPaused) 0.55f else 1f), -90f, progress * 360f, false,
                Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(timerText(shownSeconds), color = OnSurface, fontSize = 48.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.clearAndSetSemantics { contentDescription = semantic })
    }
}
