package com.studysession.tracker.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysession.tracker.R
import com.studysession.tracker.core.di.AppViewModelFactory
import com.studysession.tracker.core.theme.*
import com.studysession.tracker.core.ui.*
import com.studysession.tracker.domain.model.LoadStatus
import com.studysession.tracker.presentation.dialog.UnsavedChangesDialog

@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val model: SettingsViewModel = viewModel(factory = AppViewModelFactory())
    val state by model.state.collectAsStateWithLifecycle()
    val back by rememberUpdatedState(onBack)
    LaunchedEffect(Unit) { model.events.collect { back() } }
    BackHandler { if (state.leaveConfirmation) model.keepEditing() else model.requestBack() }
    SettingsScreen(state, model::requestBack, model::focus, model::rest, model::defaults, model::save, model::retry)
    if (state.leaveConfirmation) UnsavedChangesDialog(model::save, model::discard, model::keepEditing, state.saving)
}

@Composable
fun SettingsScreen(state: SettingsUiState, onBack: () -> Unit, onFocus: (Int) -> Unit, onRest: (Int) -> Unit,
    onDefaults: () -> Unit, onSave: () -> Unit, onRetry: () -> Unit) {
    AppBackground(BackgroundVariant.Settings) {
        ScreenColumn {
            ScreenHeader(stringResource(R.string.settings_title), onBack)
            Heading(stringResource(R.string.make_room))
            if (state.loadStatus == LoadStatus.Loading) StatusPanel(R.string.loading_settings, loading = true)
            state.errorRes?.let { StatusPanel(it, R.string.retry, onRetry) }
            NotebookCard {
                DurationStepper(stringResource(R.string.focus_length), stringResource(R.string.duration_minutes, state.draft.focusMinutes),
                    { onFocus(-5) }, { onFocus(5) }, state.draft.focusMinutes > 5 && !state.saving, state.draft.focusMinutes < 90 && !state.saving)
                MutedText(stringResource(R.string.focus_range))
            }
            NotebookCard {
                DurationStepper(stringResource(R.string.short_break), stringResource(R.string.duration_minutes, state.draft.breakMinutes),
                    { onRest(-1) }, { onRest(1) }, state.draft.breakMinutes > 1 && !state.saving, state.draft.breakMinutes < 30 && !state.saving)
                MutedText(stringResource(R.string.break_range))
            }
            NotebookCard {
                Text(stringResource(R.string.stored_on_device), color = Accent, style = MaterialTheme.typography.titleLarge)
                MutedText(stringResource(R.string.privacy_hint))
            }
            MutedText(stringResource(R.string.next_session_hint))
            PrimaryButton(if (state.errorRes == null) R.string.save_settings else R.string.retry_save, onSave,
                enabled = state.hasChanges && !state.saving, loading = state.saving)
            SecondaryButton(R.string.restore_defaults, onDefaults, Modifier.fillMaxWidth(), enabled = !state.saving)
        }
    }
}
