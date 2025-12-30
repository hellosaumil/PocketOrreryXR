package io.hellosaumil.pocketorrery

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.offset
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("RestrictedApi")
@Composable
fun SolarSystemScene(viewModel: SolarSystemViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Animation time state
    var animationTime by remember { mutableFloatStateOf(0f) }
    
    // Animation Loop
    LaunchedEffect(uiState.isPaused) {
        val startTime = System.nanoTime()
        while (!uiState.isPaused) {
            withFrameMillis { _ ->
                animationTime = (System.nanoTime() - startTime) / 1_000_000_000f
            }
        }
    }
    
    // Render Sun at center
    PlanetPanel(
        planet = SolarSystemRepository.sol,
        offsetX = 0.dp,
        offsetY = 0.dp,
        offsetZ = 0.dp
    )
    
    // Render Planets
    SolarSystemRepository.planets.forEach { planet ->
        val angle = animationTime * planet.orbitSpeed * 0.5f
        val scaleFactor = 100f // Scale to make distances visible in 3D (dp units)
        val x = cos(angle) * planet.orbitDistance * scaleFactor
        val z = sin(angle) * planet.orbitDistance * scaleFactor
        
        PlanetPanel(
            planet = planet,
            offsetX = x.dp,
            offsetY = 0.dp,
            offsetZ = z.dp
        )
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun PlanetPanel(planet: Planet, offsetX: Dp, offsetY: Dp, offsetZ: Dp) {
    SpatialPanel(
        modifier = SubspaceModifier.offset(offsetX, offsetY, offsetZ)
    ) {
        Box(
            modifier = Modifier
                .size((planet.radius * 200).dp)
                .background(planet.color, CircleShape)
        )
    }
}
