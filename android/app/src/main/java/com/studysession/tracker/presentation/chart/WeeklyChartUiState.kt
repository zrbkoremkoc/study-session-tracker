package com.studysession.tracker.presentation.chart

import com.studysession.tracker.domain.model.*
import java.time.LocalDate

data class ChartRow(val subject: Subject, val focusSeconds: Long, val goalSeconds: Long, val fraction: Float, val goalFraction: Float, val percent: Int)
data class WeeklyChartUiState(
    val loadStatus: LoadStatus = LoadStatus.Loading,
    val week: LocalDate = LocalDate.now(),
    val rows: List<ChartRow> = emptyList(),
    val summary: WeekSummary = WeekSummary(),
    val scaleSeconds: Long = 1800,
    val canGoNext: Boolean = false,
    val showExample: Boolean = false,
    val selectedSubjectId: String? = null,
    val errorRes: Int? = null
)
