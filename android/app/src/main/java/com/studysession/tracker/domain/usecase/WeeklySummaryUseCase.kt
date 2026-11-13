package com.studysession.tracker.domain.usecase

import com.studysession.tracker.domain.model.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class WeeklySummaryUseCase {
    fun currentMonday(today: LocalDate = LocalDate.now()): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun calculate(sessions: List<StudySession>, week: LocalDate, zone: ZoneId = ZoneId.systemDefault()): WeekSummary {
        val totals = mutableMapOf<String, Long>()
        val days = mutableSetOf<LocalDate>()
        sessions.forEach { session ->
            session.segments.forEach { segment ->
                var cursor = Instant.ofEpochMilli(segment.startedAt)
                var remaining = segment.seconds
                while (remaining > 0) {
                    val date = cursor.atZone(zone).toLocalDate()
                    val nextDay = date.plusDays(1).atStartOfDay(zone).toInstant()
                    val untilBoundary = ((nextDay.toEpochMilli() - cursor.toEpochMilli() + 999) / 1000).coerceAtLeast(1)
                    val credit = minOf(remaining, untilBoundary)
                    if (!date.isBefore(week) && date.isBefore(week.plusDays(7))) {
                        totals[session.subjectId] = (totals[session.subjectId] ?: 0) + credit
                        days.add(date)
                    }
                    remaining -= credit
                    cursor = cursor.plusSeconds(credit)
                }
            }
        }
        return WeekSummary(totals.values.sum(), days.size, totals.toMap())
    }
}
