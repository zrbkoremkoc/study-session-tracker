package com.studysession.tracker.data.repository

import com.studysession.tracker.data.local.StudyPreferences
import com.studysession.tracker.data.sample.SampleData
import com.studysession.tracker.domain.model.*
import com.studysession.tracker.domain.repository.StudyRepository
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class StudyRepositoryImpl(
    private val preferences: StudyPreferences,
    private val clock: StudyClock,
    private val starterNames: List<String>
) : StudyRepository {
    private val mutableState = MutableStateFlow(StoreState())
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var hasData = false

    override suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val result = preferences.read()
                val data = result.data ?: StudyData(subjects = SampleData.subjects(starterNames, clock.wallMillis()), selectedSubjectId = "starter_0")
                if (result.data == null) preferences.write(data)
                hasData = true
                mutableState.update { it.copy(data = data, status = LoadStatus.Ready, readFailed = result.recovered) }
            } catch (_: Exception) {
                mutableState.update { it.copy(status = LoadStatus.Error, readFailed = true) }
            }
        }
    }

    private suspend fun change(transform: (StudyData) -> StudyData): StudyData = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(hasData)
            val next = transform(state.value.data)
            preferences.write(next)
            mutableState.update { it.copy(data = next, status = LoadStatus.Ready, readFailed = false) }
            next
        }
    }

    override suspend fun selectSubject(id: String) {
        change { data -> require(data.subjects.any { it.id == id && !it.archived }); data.copy(selectedSubjectId = id) }
    }

    override suspend fun saveSubject(subject: Subject) {
        change { data ->
            val name = subject.name.trim()
            require(name.codePointCount(0, name.length) in 1..32)
            require(data.subjects.none { !it.archived && it.id != subject.id && it.name.equals(name, true) })
            require(data.subjects.any { it.id == subject.id } || data.subjects.count { !it.archived } < 12)
            val updated = subject.copy(name = name, weeklyGoalMinutes = subject.weeklyGoalMinutes.coerceIn(0, 2400))
            data.copy(subjects = data.subjects.filterNot { it.id == subject.id } + updated,
                selectedSubjectId = data.selectedSubjectId ?: subject.id)
        }
    }

    override suspend fun archiveSubject(id: String) {
        change { data ->
            val subjects = data.subjects.map { if (it.id == id) it.copy(archived = true) else it }
            data.copy(subjects = subjects, selectedSubjectId = if (data.selectedSubjectId == id) subjects.firstOrNull { !it.archived }?.id else data.selectedSubjectId)
        }
    }

    override suspend fun saveGoals(goals: Map<String, Int>) {
        change { data -> data.copy(subjects = data.subjects.map { it.copy(weeklyGoalMinutes = (goals[it.id] ?: it.weeklyGoalMinutes).coerceIn(0, 2400)) }) }
    }

    override suspend fun saveSettings(settings: TimerSettings) {
        require(settings.focusMinutes in 5..90 && settings.breakMinutes in 1..30)
        change { it.copy(settings = settings) }
    }

    override suspend fun createDraft(subjectId: String): SessionDraft {
        val data = change { data ->
            check(data.activeDraft == null)
            val subject = data.subjects.first { it.id == subjectId && !it.archived }
            data.copy(activeDraft = SessionDraft(UUID.randomUUID().toString(), subject.id, subject.name,
                clock.wallMillis(), data.settings.focusMinutes * 60L, data.settings.breakMinutes * 60L))
        }
        return checkNotNull(data.activeDraft)
    }

    override suspend fun checkpoint(draft: SessionDraft) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (state.value.data.activeDraft?.id == draft.id) {
                preferences.checkpoint(draft)
                mutableState.update { it.copy(data = it.data.copy(activeDraft = draft)) }
            }
        }
    }

    override suspend fun discardDraft() { change { it.copy(activeDraft = null) } }

    override suspend fun finish(draft: SessionDraft): StudySession {
        val data = change { data ->
            if (data.sessions.any { it.id == draft.id }) data else {
                require(draft.focusSeconds > 0)
                require(draft.segments.sumOf { it.seconds } == draft.focusSeconds)
                val session = StudySession(draft.id, draft.subjectId, draft.subjectName, draft.startedAt,
                    clock.wallMillis(), draft.focusSeconds, draft.breakSeconds,
                    draft.plannedFocusSeconds, draft.plannedBreakSeconds,
                    if (draft.focusSeconds >= draft.plannedFocusSeconds) CompletionReason.FocusCompleted else CompletionReason.FinishedEarly,
                    draft.segments)
                data.copy(sessions = data.sessions + session, activeDraft = null)
            }
        }
        return data.sessions.first { it.id == draft.id }
    }

    override suspend fun deleteSession(id: String) { change { it.copy(sessions = it.sessions.filterNot { session -> session.id == id }) } }
}
