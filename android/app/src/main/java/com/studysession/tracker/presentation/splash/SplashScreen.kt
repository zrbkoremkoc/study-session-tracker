package com.studysession.tracker.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysession.tracker.R
import com.studysession.tracker.core.di.AppViewModelFactory
import com.studysession.tracker.core.theme.*
import com.studysession.tracker.core.ui.*

@Composable
fun SplashRoute(onFinished: () -> Unit) {
    val model: SplashViewModel = viewModel(factory = AppViewModelFactory())
    val state by model.state.collectAsStateWithLifecycle()
    val finished by model.finished.collectAsStateWithLifecycle()
    val latestFinished by rememberUpdatedState(finished)
    val callback by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) { model.events.collect { if (latestFinished || model.finished.value) callback() } }
    SplashScreen(state)
}

@Composable
fun SplashScreen(state: SplashUiState) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val transition = rememberInfiniteTransition(label = "desk float")
    val float by transition.animateFloat(-5f, 5f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "float")
    val halo by transition.animateFloat(0.10f, 0.18f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "halo")
    val scale by animateFloatAsState(if (entered && state.decorativeEntryVisible) 1f else 0.94f, tween(450), label = "entry")
    val entryAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(450), label = "entry alpha")
    val opening = stringResource(R.string.opening)
    AppBackground(BackgroundVariant.Splash) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.splash_eyebrow), style = MaterialTheme.typography.labelSmall, color = Accent)
            Spacer(Modifier.height(32.dp))
            Box(Modifier.size(224.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(212.dp).background(Brush.radialGradient(listOf(Accent.copy(alpha = halo), Accent.copy(alpha = 0f))), CircleShape))
                Image(painterResource(R.drawable.logo), null, Modifier.size(176.dp).offset(y = float.dp).scale(scale).alpha(entryAlpha), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.splash_title), Modifier.alpha(entryAlpha).semantics { heading() }, color = OnSurface,
                style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            MutedText(stringResource(R.string.splash_subtitle))
            Spacer(Modifier.height(40.dp))
            LinearProgressIndicator(Modifier.width(160.dp).height(4.dp).clip(RoundedCornerShape(4.dp))
                .semantics { contentDescription = opening }, color = Accent, trackColor = OnSurface.copy(alpha = 0.12f))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.opening_short), style = MaterialTheme.typography.labelSmall, color = OnSurface.copy(alpha = 0.64f))
        }
    }
}
