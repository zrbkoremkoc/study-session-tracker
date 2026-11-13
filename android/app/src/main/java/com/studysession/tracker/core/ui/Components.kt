package com.studysession.tracker.core.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.studysession.tracker.R
import com.studysession.tracker.core.theme.*

@Composable
fun NotebookCard(modifier: Modifier = Modifier, selected: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val outline by animateColorAsState(if (selected) Accent else OnSurface.copy(alpha = 0.10f), tween(160), label = "outline")
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Secondary.copy(alpha = 0.78f),
        border = BorderStroke(if (selected) 2.dp else 1.dp, outline), shadowElevation = 4.dp) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium, color = OnSurface)
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = OnSurface.copy(alpha = 0.76f))
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onBack, Modifier.heightIn(min = 48.dp)) {
            Image(painterResource(R.drawable.icon_home), null, Modifier.size(20.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.back))
        }
        Heading(title, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(labelRes: Int, formattedValue: String, modifier: Modifier = Modifier) {
    NotebookCard(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(formattedValue, style = MaterialTheme.typography.headlineMedium, color = Accent)
        MutedText(stringResource(labelRes))
    }
}

@Composable
fun DurationStepper(label: String, value: String, onDecrease: () -> Unit, onIncrease: () -> Unit,
    decreaseEnabled: Boolean, increaseEnabled: Boolean) {
    val decrease = stringResource(R.string.decrease_value, label)
    val increase = stringResource(R.string.increase_value, label)
    val semantic = stringResource(R.string.value_state, label, value)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.semantics { stateDescription = semantic }) {
        Text(label, color = OnSurface, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onDecrease, Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp).semantics { contentDescription = decrease }, enabled = decreaseEnabled,
                contentPadding = PaddingValues(8.dp)) { Text(stringResource(R.string.minus)) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Crossfade(value, animationSpec = tween(150), label = "value") { Text(it, style = MaterialTheme.typography.titleLarge, color = OnSurface) }
            }
            OutlinedButton(onIncrease, Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp).semantics { contentDescription = increase }, enabled = increaseEnabled,
                contentPadding = PaddingValues(8.dp)) { Text(stringResource(R.string.plus)) }
        }
    }
}

@Composable
fun StatusPanel(messageRes: Int, actionRes: Int? = null, onAction: (() -> Unit)? = null, loading: Boolean = false) {
    NotebookCard {
        Text(stringResource(messageRes), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = OnSurface)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent, trackColor = OnSurface.copy(alpha = 0.12f))
        if (actionRes != null && onAction != null) SecondaryButton(actionRes, onAction)
    }
}

@Composable
fun GoalProgressCard(subjectName: String, focusText: String, goalText: String, progress: Float, reached: Boolean) {
    NotebookCard {
        Text(subjectName, color = OnSurface, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.week_goal_total, focusText, goalText), color = OnSurface)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Accent, trackColor = OnSurface.copy(alpha = 0.12f))
        if (reached) Text(stringResource(R.string.goal_reached), color = Accent)
    }
}

@Composable
fun SaveCheckmark(modifier: Modifier = Modifier) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(if (entered) 1f else 0f, tween(220), label = "saved")
    Canvas(modifier.size(40.dp)) {
        drawCircle(Accent.copy(alpha = 0.14f))
        val path = Path().apply {
            moveTo(size.width * 0.25f, size.height * 0.5f)
            lineTo(size.width * 0.43f, size.height * 0.68f)
            lineTo(size.width * (0.43f + 0.34f * reveal), size.height * (0.68f - 0.4f * reveal))
        }
        drawPath(path, Accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}
