package com.studysession.tracker.core.di

import android.content.Context
import com.studysession.tracker.R
import com.studysession.tracker.data.local.PlatformStudyClock
import com.studysession.tracker.data.local.StudyPreferences
import com.studysession.tracker.data.repository.StudyRepositoryImpl
import com.studysession.tracker.domain.model.StudyClock
import com.studysession.tracker.domain.repository.StudyRepository
import com.studysession.tracker.domain.usecase.WeeklySummaryUseCase
import com.studysession.tracker.domain.model.Subject
import com.studysession.tracker.data.sample.SampleData

object ServiceLocator {
    val clock: StudyClock = PlatformStudyClock()
    val weeklySummary = WeeklySummaryUseCase()
    private var instance: StudyRepository? = null
    val study: StudyRepository get() = checkNotNull(instance)
    var exampleSubjects: List<Subject> = emptyList()
        private set

    fun initialize(context: Context) {
        if (instance != null) return
        val names = listOf(R.string.mathematics, R.string.biology, R.string.history, R.string.english).map { context.getString(it) }
        exampleSubjects = SampleData.subjects(names, clock.wallMillis())
        instance = StudyRepositoryImpl(StudyPreferences(context.applicationContext), clock, names)
    }
}
