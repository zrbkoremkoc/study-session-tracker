package com.studysession.tracker.domain.model

interface StudyClock {
    fun monotonicMillis(): Long
    fun wallMillis(): Long
}
