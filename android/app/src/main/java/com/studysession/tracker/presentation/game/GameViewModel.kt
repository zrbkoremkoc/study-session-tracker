package com.studysession.tracker.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studysession.tracker.R
import com.studysession.tracker.core.config.GameConfig
import com.studysession.tracker.domain.model.*
import com.studysession.tracker.domain.repository.StudyRepository
import com.studysession.tracker.domain.usecase.SessionTimerUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(private val study: StudyRepository, private val timer: SessionTimerUseCase) : ViewModel() {
    private val mutableState = MutableStateFlow(GameUiState())
    val state = mutableState.asStateFlow()
    private val channel = Channel<GameEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private var ticker: Job? = null
    private var attached = false
    private var lastCheckpoint = -1L

    fun start(subjectId: String) {
        if (state.value.creating || state.value.saveStatus == SaveStatus.Saving) return
        mutableState.update { it.copy(creating = true, errorRes = null) }
        viewModelScope.launch {
            try {
                val draft = study.createDraft(subjectId)
                timer.resume(draft)
                mutableState.value = GameUiState(draft = draft, remainingSeconds = draft.plannedFocusSeconds, isPaused = false)
                lastCheckpoint = -1
                channel.send(GameEvent.Open(draft.id))
            } catch (_: Exception) { mutableState.update { it.copy(creating = false, errorRes = R.string.start_error) } }
        }
    }

    fun openExisting() {
        val draft = study.state.value.data.activeDraft ?: return
        ticker?.cancel()
        timer.pause(state.value.draft ?: draft)
        mutableState.value = GameUiState(draft = draft, restored = true)
        render(draft)
        viewModelScope.launch { channel.send(GameEvent.Open(draft.id)) }
    }

    fun replaceDraft(subjectId: String) {
        if (state.value.creating) return
        pause()
        mutableState.update { it.copy(creating = true) }
        viewModelScope.launch {
            try { study.discardDraft(); mutableState.update { it.copy(creating = false) }; start(subjectId) }
            catch (_: Exception) { mutableState.update { it.copy(creating = false, errorRes = R.string.write_error) } }
        }
    }

    fun saveExisting() { openExisting(); save() }

    fun attach() {
        attached = true
        if (timer.running && !state.value.completed) {
            state.value.draft?.let { render(timer.snapshot(it)) }
            if (state.value.draft?.phase == Phase.Completed) save() else startTicker()
        }
    }

    fun pause(preserveForRotation: Boolean = false) {
        attached = false
        ticker?.cancel()
        ticker = null
        if (preserveForRotation) return
        val draft = state.value.draft ?: return
        if (state.value.completed) return
        val paused = timer.pause(draft)
        render(paused)
        mutableState.update { it.copy(isPaused = true) }
        checkpoint(paused)
    }

    fun foregroundLost(changingConfigurations: Boolean) { pause(changingConfigurations) }

    fun togglePause() {
        val draft = state.value.draft ?: return
        if (state.value.saveStatus == SaveStatus.Saving || state.value.completed) return
        if (timer.running) {
            val paused = timer.pause(draft)
            ticker?.cancel()
            render(paused)
            mutableState.update { it.copy(isPaused = true) }
            checkpoint(paused)
        } else if (draft.phase == Phase.Focus || draft.phase == Phase.Break) {
            timer.resume(draft)
            mutableState.update { it.copy(isPaused = false, restored = false, errorRes = null) }
            startTicker()
        }
    }

    fun startBreak() {
        val draft = state.value.draft ?: return
        if (draft.phase != Phase.BreakReady) return
        val rest = draft.copy(phase = Phase.Break)
        timer.resume(rest)
        render(rest)
        mutableState.update { it.copy(isPaused = false) }
        checkpoint(rest)
        startTicker()
    }

    fun requestExit() {
        if (state.value.completed) { viewModelScope.launch { channel.send(GameEvent.Back) }; return }
        if (state.value.saveStatus == SaveStatus.Saving) return
        val draft = state.value.draft ?: return
        val frozen = timer.pause(draft)
        ticker?.cancel()
        render(frozen)
        checkpoint(frozen)
        mutableState.update { it.copy(isPaused = true, exitConfirmation = true) }
    }

    fun dismissExit(resume: Boolean = false) {
        mutableState.update { it.copy(exitConfirmation = false) }
        if (resume) togglePause()
    }

    fun keepPaused() { mutableState.update { it.copy(saveStatus = SaveStatus.Idle, errorRes = null, isPaused = true) } }

    fun discard() {
        if (state.value.saveStatus == SaveStatus.Saving) return
        pause()
        mutableState.update { it.copy(saveStatus = SaveStatus.Saving) }
        viewModelScope.launch {
            try {
                study.discardDraft()
                mutableState.value = GameUiState()
                channel.send(GameEvent.Back)
            } catch (_: Exception) { mutableState.update { it.copy(saveStatus = SaveStatus.Failed, errorRes = R.string.write_error) } }
        }
    }

    fun save() {
        val draft = state.value.draft ?: return
        if (state.value.saveStatus == SaveStatus.Saving || draft.focusSeconds < 1 || state.value.completed) return
        val frozen = timer.pause(draft)
        ticker?.cancel()
        render(frozen)
        mutableState.update { it.copy(saveStatus = SaveStatus.Saving, isPaused = true, exitConfirmation = false, errorRes = null) }
        viewModelScope.launch {
            try {
                val session = study.finish(frozen)
                mutableState.update { it.copy(saveStatus = SaveStatus.Saved, completed = true, draft = frozen.copy(phase = Phase.Completed)) }
                delay(220)
                channel.send(GameEvent.Result(session.id, true))
            } catch (_: Exception) { mutableState.update { it.copy(saveStatus = SaveStatus.Failed, errorRes = R.string.session_save_error) } }
        }
    }

    fun viewResult() { state.value.draft?.let { viewModelScope.launch { channel.send(GameEvent.Result(it.id)) } } }

    private fun startTicker() {
        ticker?.cancel()
        if (!attached || !timer.running) return
        ticker = viewModelScope.launch {
            while (isActive && timer.running) {
                delay(GameConfig.TICK_MS)
                val current = state.value.draft ?: break
                val next = timer.snapshot(current)
                render(next)
                val stamp = next.focusSeconds + next.breakSeconds
                if (stamp != lastCheckpoint) { lastCheckpoint = stamp; checkpoint(next) }
                if (!timer.running) {
                    mutableState.update { it.copy(isPaused = true) }
                    if (next.phase == Phase.Completed) save()
                }
            }
        }
    }

    private fun render(draft: SessionDraft) {
        val planned = if (draft.phase == Phase.Break) draft.plannedBreakSeconds * 1000 else draft.plannedFocusSeconds * 1000
        val elapsed = if (draft.phase == Phase.Break) draft.breakMs else draft.focusMs
        mutableState.update { it.copy(draft = draft, remainingSeconds = ((planned - elapsed).coerceAtLeast(0) + 999) / 1000,
            progress = (elapsed.toFloat() / planned.coerceAtLeast(1)).coerceIn(0f, 1f), isPaused = !timer.running) }
    }

    private fun checkpoint(draft: SessionDraft) {
        viewModelScope.launch {
            try { study.checkpoint(draft) }
            catch (_: Exception) { mutableState.update { it.copy(errorRes = R.string.checkpoint_error) } }
        }
    }
}
