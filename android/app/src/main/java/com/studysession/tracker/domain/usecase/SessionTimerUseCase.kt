package com.studysession.tracker.domain.usecase

import com.studysession.tracker.domain.model.*

class SessionTimerUseCase(private val clock: StudyClock) {
    private var anchorMono = 0L
    private var anchorWall = 0L
    private var base: SessionDraft? = null
    var running = false
        private set

    fun resume(draft: SessionDraft) {
        base = draft
        anchorMono = clock.monotonicMillis()
        anchorWall = clock.wallMillis()
        running = draft.phase == Phase.Focus || draft.phase == Phase.Break
    }

    fun snapshot(fallback: SessionDraft): SessionDraft {
        val start = base ?: return fallback
        if (!running) return fallback
        val elapsed = (clock.monotonicMillis() - anchorMono).coerceAtLeast(0)
        val actual = if (start.phase == Phase.Focus) {
            elapsed.coerceAtMost(start.plannedFocusSeconds * 1000 - start.focusMs)
        } else {
            elapsed.coerceAtMost(start.plannedBreakSeconds * 1000 - start.breakMs)
        }.coerceAtLeast(0)
        val seconds = if (start.phase == Phase.Focus) (start.focusMs + actual) / 1000 - start.focusSeconds else 0
        val segments = if (seconds > 0) start.segments + FocusSegment(anchorWall, seconds) else start.segments
        val next = if (start.phase == Phase.Focus) {
            val focus = start.focusMs + actual
            start.copy(focusMs = focus, segments = segments,
                phase = if (focus >= start.plannedFocusSeconds * 1000) Phase.BreakReady else Phase.Focus)
        } else {
            val rest = start.breakMs + actual
            start.copy(breakMs = rest,
                phase = if (rest >= start.plannedBreakSeconds * 1000) Phase.Completed else Phase.Break)
        }
        val drift = clock.wallMillis() - (anchorWall + elapsed)
        if (kotlin.math.abs(drift) > 2000 && next.phase == start.phase) {
            base = next
            anchorMono = clock.monotonicMillis()
            anchorWall = clock.wallMillis()
        }
        if (next.phase != start.phase) running = false
        return next
    }

    fun pause(draft: SessionDraft): SessionDraft {
        val result = snapshot(draft)
        running = false
        base = result
        return result
    }
}
