package com.studysession.tracker.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.studysession.tracker.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun durationText(seconds: Long): String = when {
    seconds < 60 -> stringResource(R.string.duration_seconds, seconds)
    seconds < 3600 -> stringResource(R.string.duration_minutes, seconds / 60)
    seconds % 3600 < 60 -> stringResource(R.string.duration_hours, seconds / 3600)
    else -> stringResource(R.string.duration_hours_minutes, seconds / 3600, seconds % 3600 / 60)
}

@Composable
fun goalText(seconds: Long): String = if (seconds <= 0) stringResource(R.string.no_goal) else durationText(seconds)

fun dateTimeText(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.getDefault()))

fun dateText(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))

fun timerText(seconds: Long): String = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
