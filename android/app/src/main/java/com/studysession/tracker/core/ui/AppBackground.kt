package com.studysession.tracker.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.studysession.tracker.R
import com.studysession.tracker.core.theme.*

enum class BackgroundVariant { Splash, Desk, Timer, Result, Journal, Chart, Plan, Settings }

@Composable
fun AppBackground(variant: BackgroundVariant, content: @Composable BoxScope.() -> Unit) {
    val colors = when (variant) {
        BackgroundVariant.Splash -> listOf(Background, Secondary.copy(alpha = 0.87f), Background.copy(alpha = 0.98f))
        BackgroundVariant.Result, BackgroundVariant.Plan -> listOf(Secondary.copy(alpha = 0.94f), Background.copy(alpha = 0.93f), Background)
        BackgroundVariant.Timer, BackgroundVariant.Chart -> listOf(Background.copy(alpha = 0.94f), Background.copy(alpha = 0.89f), Secondary.copy(alpha = 0.96f))
        else -> listOf(Background.copy(alpha = 0.93f), Secondary.copy(alpha = 0.88f), Background.copy(alpha = 0.98f))
    }
    Box(Modifier.fillMaxSize().background(Background)) {
        Image(painterResource(R.drawable.bg_game), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors)))
        Canvas(Modifier.fillMaxSize()) {
            val spacing = if (variant == BackgroundVariant.Chart) 24.dp.toPx() else 28.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawLine(OnSurface.copy(alpha = 0.035f), Offset(0f, y), Offset(size.width, y), 1f)
                y += spacing
            }
            if (variant == BackgroundVariant.Chart) {
                var x = 0f
                while (x < size.width) {
                    drawLine(OnSurface.copy(alpha = 0.025f), Offset(x, 0f), Offset(x, size.height), 1f)
                    x += spacing
                }
            }
            drawCircle(brush = Brush.radialGradient(listOf(Accent.copy(alpha = 0.065f), Accent.copy(alpha = 0f)),
                center = Offset(size.width * 0.85f, size.height * 0.15f), radius = size.width * 0.8f),
                radius = size.width * 0.8f, center = Offset(size.width * 0.85f, size.height * 0.15f))
        }
        content()
    }
}

@Composable
fun ScreenColumn(footer: (@Composable ColumnScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        if (footer != null && maxHeight >= 580.dp) {
            Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().fillMaxHeight()) {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp), content = footer)
            }
        } else {
            Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                content()
                footer?.invoke(this)
            }
        }
    }
}
