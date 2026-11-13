package com.studysession.tracker.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studysession.tracker.core.theme.*

@Composable
fun PrimaryButton(textRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, iconRes: Int? = null) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, spring(dampingRatio = 0.85f, stiffness = 500f), label = "press")
    val shape = RoundedCornerShape(18.dp)
    Button(onClick = onClick, enabled = enabled && !loading, interactionSource = interactions,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp).scale(scale)
            .shadow(if (enabled) 6.dp else 0.dp, shape, ambientColor = Accent, spotColor = Accent)
            .background(Brush.horizontalGradient(if (enabled) listOf(Accent, Accent.copy(alpha = 0.88f)) else listOf(Secondary, Secondary)), shape),
        shape = shape, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Background,
            disabledContainerColor = Color.Transparent, disabledContentColor = OnSurface.copy(alpha = 0.55f)),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        if (loading) { CircularProgressIndicator(Modifier.size(20.dp), color = Background, strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)) }
        if (iconRes != null) { Image(painterResource(iconRes), null, Modifier.size(24.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(8.dp)) }
        Text(stringResource(textRes), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(textRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OnSurface.copy(alpha = if (enabled) 0.18f else 0.06f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Secondary.copy(alpha = 0.72f), contentColor = OnSurface,
            disabledContainerColor = Secondary.copy(alpha = 0.3f), disabledContentColor = OnSurface.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Text(stringResource(textRes), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}
