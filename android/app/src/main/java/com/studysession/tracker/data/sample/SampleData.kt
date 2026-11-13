package com.studysession.tracker.data.sample

import com.studysession.tracker.domain.model.*
import java.time.LocalDate
import java.time.ZoneId

object SampleData {
    fun subjects(names: List<String>, now: Long): List<Subject> =
        names.mapIndexed { index, name -> Subject("starter_$index", name, listOf(180, 120, 90, 90)[index], now) }

    fun example(subjects: List<Subject>, week: LocalDate): List<StudySession> {
        val specs = listOf(
            listOf(0, 16, 0, 0, 50, 5, 50),
            listOf(1, 15, 30, 1, 40, 5, 40),
            listOf(1, 17, 0, 3, 25, 0, 25),
            listOf(2, 16, 0, 0, 25, 5, 25),
            listOf(3, 16, 30, 2, 30, 5, 30),
            listOf(4, 15, 0, 1, 35, 0, 40),
            listOf(5, 10, 0, 3, 20, 0, 25)
        )
        return specs.mapIndexed { index, spec ->
            val subject = subjects[spec[3]]
            val start = week.plusDays(spec[0].toLong()).atTime(spec[1], spec[2]).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            StudySession("example_$index", subject.id, subject.name, start,
                start + (spec[4] + spec[5]) * 60_000L, spec[4] * 60L, spec[5] * 60L,
                spec[6] * 60L, 300L,
                if (spec[4] == spec[6]) CompletionReason.FocusCompleted else CompletionReason.FinishedEarly,
                listOf(FocusSegment(start, spec[4] * 60L)))
        }
    }
}
