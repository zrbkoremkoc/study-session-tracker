package com.studysession.tracker.domain.model

data class Subject(
    val id: String,
    val name: String,
    val weeklyGoalMinutes: Int,
    val createdAt: Long,
    val archived: Boolean = false
)

data class TimerSettings(val focusMinutes: Int = 25, val breakMinutes: Int = 5)
enum class Phase { Focus, BreakReady, Break, Completed }
enum class CompletionReason { FocusCompleted, FinishedEarly }
enum class LoadStatus { Loading, Ready, Error }
enum class SaveStatus { Idle, Saving, Failed, Saved }

data class FocusSegment(val startedAt: Long, val seconds: Long)

data class SessionDraft(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val startedAt: Long,
    val plannedFocusSeconds: Long,
    val plannedBreakSeconds: Long,
    val focusMs: Long = 0,
    val breakMs: Long = 0,
    val phase: Phase = Phase.Focus,
    val segments: List<FocusSegment> = emptyList()
) {
    val focusSeconds: Long get() = focusMs / 1000
    val breakSeconds: Long get() = breakMs / 1000
}

data class StudySession(
    val id: String,
    val subjectId: String,
    val subjectNameSnapshot: String,
    val startedAt: Long,
    val endedAt: Long,
    val focusSeconds: Long,
    val breakSeconds: Long,
    val plannedFocusSeconds: Long,
    val plannedBreakSeconds: Long,
    val completionReason: CompletionReason,
    val segments: List<FocusSegment>
)

data class StudyData(
    val subjects: List<Subject> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val settings: TimerSettings = TimerSettings(),
    val selectedSubjectId: String? = null,
    val activeDraft: SessionDraft? = null
)

data class StoreState(
    val data: StudyData = StudyData(),
    val status: LoadStatus = LoadStatus.Loading,
    val readFailed: Boolean = false
)

data class SubjectWeek(val subject: Subject, val focusSeconds: Long)
data class WeekSummary(
    val totalSeconds: Long = 0,
    val activeDays: Int = 0,
    val bySubject: Map<String, Long> = emptyMap()
)
