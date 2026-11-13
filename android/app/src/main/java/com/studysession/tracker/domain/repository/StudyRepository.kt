package com.studysession.tracker.domain.repository

import com.studysession.tracker.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface StudyRepository {
    val state: StateFlow<StoreState>
    suspend fun load()
    suspend fun selectSubject(id: String)
    suspend fun saveSubject(subject: Subject)
    suspend fun archiveSubject(id: String)
    suspend fun saveGoals(goals: Map<String, Int>)
    suspend fun saveSettings(settings: TimerSettings)
    suspend fun createDraft(subjectId: String): SessionDraft
    suspend fun checkpoint(draft: SessionDraft)
    suspend fun discardDraft()
    suspend fun finish(draft: SessionDraft): StudySession
    suspend fun deleteSession(id: String)
}
