package com.studysession.tracker.presentation.chart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysession.tracker.R
import com.studysession.tracker.core.di.AppViewModelFactory
import com.studysession.tracker.core.theme.*
import com.studysession.tracker.core.ui.*
import com.studysession.tracker.domain.model.LoadStatus

@Composable
fun WeeklyChartRoute(onBack: () -> Unit, onStart: () -> Unit, onGoals: () -> Unit) {
    val model: WeeklyChartViewModel = viewModel(factory = AppViewModelFactory())
    val state by model.state.collectAsStateWithLifecycle()
    WeeklyChartScreen(state, onBack, onStart, onGoals, model::previous, model::next, model::example, model::personal, model::select, model::retry)
}

@Composable
fun WeeklyChartScreen(state: WeeklyChartUiState, onBack: () -> Unit, onStart: () -> Unit, onGoals: () -> Unit,
    onPrevious: () -> Unit, onNext: () -> Unit, onExample: () -> Unit, onPersonal: () -> Unit, onSelect: (String) -> Unit, onRetry: () -> Unit) {
    val weekLabel = stringResource(R.string.week_range, dateText(state.week), dateText(state.week.plusDays(6)))
    AppBackground(BackgroundVariant.Chart) {
        ScreenColumn {
            ScreenHeader(stringResource(R.string.weekly_chart_title), onBack)
            if (state.showExample) {
                NotebookCard(selected = true) {
                    Text(stringResource(R.string.example_banner), color = Accent, style = MaterialTheme.typography.titleLarge)
                    MutedText(stringResource(R.string.example_hint))
                    SecondaryButton(R.string.return_my_data, onPersonal, Modifier.fillMaxWidth())
                }
            }
            Text(weekLabel, style = MaterialTheme.typography.titleLarge, color = OnSurface, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(R.string.previous_week, onPrevious, Modifier.weight(1f), enabled = !state.showExample)
                val nextDescription = stringResource(if (state.canGoNext) R.string.next_week else R.string.current_week)
                SecondaryButton(R.string.next_week, onNext, Modifier.weight(1f).semantics { stateDescription = nextDescription }, enabled = state.canGoNext)
            }
            if (state.loadStatus == LoadStatus.Loading) StatusPanel(R.string.loading_chart, loading = true)
            state.errorRes?.let { StatusPanel(it, R.string.retry, onRetry) }
            if (state.summary.totalSeconds > 0 && state.summary.activeDays > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(R.string.study_this_week, durationText(state.summary.totalSeconds), Modifier.weight(1f))
                    StatCard(R.string.active_days, state.summary.activeDays.toString(), Modifier.weight(1f))
                }
            } else if (state.loadStatus != LoadStatus.Loading) {
                StatusPanel(R.string.empty_week, R.string.start_session, onStart)
                if (!state.showExample) SecondaryButton(R.string.view_example, onExample, Modifier.fillMaxWidth())
            }
            NotebookCard {
                Text(stringResource(R.string.time_by_subject), style = MaterialTheme.typography.titleLarge, color = OnSurface, modifier = Modifier.semantics { heading() })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MutedText(stringResource(R.string.axis_zero))
                    MutedText(durationText(state.scaleSeconds / 2))
                    MutedText(durationText(state.scaleSeconds))
                }
                state.rows.forEach { row ->
                    WeeklySubjectBar(row, row.subject.id == state.selectedSubjectId, { onSelect(row.subject.id) })
                }
                if (state.rows.isEmpty()) MutedText(stringResource(R.string.no_chart_subjects))
                MutedText(stringResource(R.string.chart_legend))
            }
            MutedText(stringResource(R.string.chart_focus_hint))
            SecondaryButton(R.string.edit_goals, onGoals, Modifier.fillMaxWidth(), enabled = !state.showExample)
        }
    }
}

@Composable
private fun WeeklySubjectBar(row: ChartRow, selected: Boolean, onSelect: () -> Unit) {
    val width by animateFloatAsState(row.fraction, tween(180), label = "study bar")
    val focus = if (row.focusSeconds > 0) durationText(row.focusSeconds) else stringResource(R.string.no_sessions_yet)
    val goal = goalText(row.goalSeconds)
    val description = stringResource(R.string.chart_row_description, row.subject.name, focus, goal, row.percent)
    Column(Modifier.fillMaxWidth().semantics { stateDescription = description }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.subject.name, Modifier.weight(1f), color = OnSurface, style = MaterialTheme.typography.titleMedium)
            Text(focus, color = if (selected) Accent else OnSurface)
        }
        if (row.subject.archived) MutedText(stringResource(R.string.archived))
        Canvas(Modifier.fillMaxWidth().height(24.dp).clearAndSetSemantics { }) {
            val center = size.height / 2
            val stroke = 12.dp.toPx()
            drawLine(OnSurface.copy(alpha = 0.08f), Offset(0f, center), Offset(size.width, center), stroke, StrokeCap.Round)
            if (row.focusSeconds > 0) {
                val barWidth = (size.width * width).coerceAtLeast(8.dp.toPx()).coerceAtMost(size.width)
                drawRoundRect(if (selected) Accent else Primary, Offset(0f, center - stroke / 2), Size(barWidth, stroke), androidx.compose.ui.geometry.CornerRadius(stroke / 2))
            }
            if (row.goalSeconds > 0) {
                val x = (size.width * row.goalFraction).coerceIn(1.dp.toPx(), size.width - 1.dp.toPx())
                drawLine(OnSurface, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
            }
        }
        MutedText(if (row.goalSeconds > 0) stringResource(R.string.chart_goal_percent, goal, row.percent) else stringResource(R.string.no_goal))
        TextButton(onSelect, Modifier.heightIn(min = 48.dp)) { Text(stringResource(if (selected) R.string.selected else R.string.select)) }
        HorizontalDivider(color = OnSurface.copy(alpha = 0.08f))
    }
}
