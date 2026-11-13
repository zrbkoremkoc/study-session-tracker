package com.studysession.tracker.data.local

import android.content.Context
import com.studysession.tracker.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

class StudyPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("study_session_tracker_v1", Context.MODE_PRIVATE)

    data class ReadResult(val data: StudyData?, val recovered: Boolean = false)

    fun read(): ReadResult {
        if (preferences.all.isEmpty()) return ReadResult(null)
        return try {
            require(preferences.getInt("schemaVersion", 0) == 1)
            val json = JSONObject()
                .put("subjects", JSONArray(preferences.getString("subjects", null) ?: error("subjects")))
                .put("sessions", JSONArray(preferences.getString("sessions", null) ?: error("sessions")))
                .put("settings", JSONObject(preferences.getString("settings", null) ?: error("settings")))
                .put("selected", preferences.getString("lastSelectedSubjectId", null))
            preferences.getString("activeDraft", null)?.let { json.put("draft", JSONObject(it)) }
            ReadResult(decode(json))
        } catch (failure: Exception) {
            val backup = preferences.getString("lastValidPayload", null) ?: throw failure
            ReadResult(decode(JSONObject(backup)), true)
        }
    }

    fun write(data: StudyData) {
        val json = encode(data)
        val editor = preferences.edit()
            .putInt("schemaVersion", 1)
            .putString("subjects", json.getJSONArray("subjects").toString())
            .putString("sessions", json.getJSONArray("sessions").toString())
            .putString("settings", json.getJSONObject("settings").toString())
            .putString("lastSelectedSubjectId", data.selectedSubjectId)
            .putString("lastValidPayload", json.toString())
        if (data.activeDraft == null) editor.remove("activeDraft")
        else editor.putString("activeDraft", encodeDraft(data.activeDraft).toString())
        check(editor.commit())
    }

    fun checkpoint(draft: SessionDraft) {
        check(preferences.edit().putString("activeDraft", encodeDraft(draft).toString()).commit())
    }

    private fun encodeSegments(segments: List<FocusSegment>) = JSONArray().apply {
        segments.forEach { put(JSONObject().put("start", it.startedAt).put("seconds", it.seconds)) }
    }

    private fun decodeSegments(array: JSONArray): List<FocusSegment> = (0 until array.length()).map {
        array.getJSONObject(it).let { row -> FocusSegment(row.getLong("start"), row.getLong("seconds").coerceAtLeast(0)) }
    }

    private fun encodeDraft(draft: SessionDraft) = JSONObject()
        .put("id", draft.id).put("subjectId", draft.subjectId).put("name", draft.subjectName)
        .put("start", draft.startedAt).put("focusPlan", draft.plannedFocusSeconds)
        .put("breakPlan", draft.plannedBreakSeconds).put("focus", draft.focusSeconds * 1000)
        .put("break", draft.breakSeconds * 1000).put("phase", draft.phase.name)
        .put("segments", encodeSegments(draft.segments))

    private fun decodeDraft(json: JSONObject) = SessionDraft(
        json.getString("id"), json.getString("subjectId"), json.getString("name"), json.getLong("start"),
        json.getLong("focusPlan"), json.getLong("breakPlan"), json.getLong("focus"), json.getLong("break"),
        Phase.valueOf(json.getString("phase")), decodeSegments(json.getJSONArray("segments"))
    )

    private fun encode(data: StudyData): JSONObject {
        val subjects = JSONArray().apply {
            data.subjects.forEach { put(JSONObject().put("id", it.id).put("name", it.name)
                .put("goal", it.weeklyGoalMinutes).put("created", it.createdAt).put("archived", it.archived)) }
        }
        val sessions = JSONArray().apply {
            data.sessions.forEach { put(JSONObject().put("id", it.id).put("subjectId", it.subjectId)
                .put("name", it.subjectNameSnapshot).put("start", it.startedAt).put("end", it.endedAt)
                .put("focus", it.focusSeconds).put("break", it.breakSeconds)
                .put("focusPlan", it.plannedFocusSeconds).put("breakPlan", it.plannedBreakSeconds)
                .put("reason", it.completionReason.name).put("segments", encodeSegments(it.segments))) }
        }
        return JSONObject().put("subjects", subjects).put("sessions", sessions)
            .put("settings", JSONObject().put("focus", data.settings.focusMinutes).put("break", data.settings.breakMinutes))
            .put("selected", data.selectedSubjectId).apply { data.activeDraft?.let { put("draft", encodeDraft(it)) } }
    }

    private fun decode(json: JSONObject): StudyData {
        val subjectArray = json.getJSONArray("subjects")
        val subjects = (0 until subjectArray.length()).map { index ->
            val row = subjectArray.getJSONObject(index)
            Subject(row.getString("id"), row.getString("name"), row.getInt("goal").coerceIn(0, 2400),
                row.getLong("created"), row.getBoolean("archived"))
        }
        val sessionArray = json.getJSONArray("sessions")
        val sessions = (0 until sessionArray.length()).map { index ->
            val row = sessionArray.getJSONObject(index)
            StudySession(row.getString("id"), row.getString("subjectId"), row.getString("name"),
                row.getLong("start"), row.getLong("end"), row.getLong("focus"), row.getLong("break"),
                row.getLong("focusPlan"), row.getLong("breakPlan"), CompletionReason.valueOf(row.getString("reason")),
                decodeSegments(row.getJSONArray("segments"))).also {
                require(it.focusSeconds > 0 && it.segments.sumOf { part -> part.seconds } == it.focusSeconds)
            }
        }
        val settings = json.getJSONObject("settings")
        return StudyData(subjects, sessions, TimerSettings(settings.getInt("focus").coerceIn(5, 90),
            settings.getInt("break").coerceIn(1, 30)),
            if (json.isNull("selected")) null else json.getString("selected"), json.optJSONObject("draft")?.let(::decodeDraft))
    }
}
