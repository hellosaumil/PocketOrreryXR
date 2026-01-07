package io.hellosaumil.pocketorrery

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.LocalSpatialConfiguration
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.layout.offset
import io.hellosaumil.pocketorrery.ui.theme.PocketOrreryTheme

/**
 * Main entry point for the Pocket Orrery application.
 *
 * This activity handles the transition between 2D and Spatial (XR) modes,
 * manages the high-level UI structure, and initializes the Solar System ViewModel.
 */
class MainActivity : ComponentActivity() {

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PocketOrreryTheme {
                val viewModel: SolarSystemViewModel = viewModel()
                val spatialConfiguration = LocalSpatialConfiguration.current
                
                if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                    Subspace {
                        // The 3D Scene
                        SolarSystemScene(viewModel = viewModel)
                        
                        // Startup Text Overlay
                        StartupText3D(viewModel = viewModel)
                        
                        // The 2D Control Panel - Only show when startup is finished
                        if (viewModel.uiState.collectAsStateWithLifecycle().value.startupState == StartupState.Finished) {
                            MySpatialContent(
                                viewModel = viewModel,
                                onRequestHomeSpaceMode = spatialConfiguration::requestHomeSpaceMode
                            )
                        }
                    }
                } else {
                    My2DContent(
                        viewModel = viewModel,
                        onRequestFullSpaceMode = spatialConfiguration::requestFullSpaceMode
                    )
                }
            }
        }
    }
}

/**
 * Container for UI elements when the app is running in Spatial (XR) mode.
 *
 * @param viewModel The shared Solar System view model.
 * @param onRequestHomeSpaceMode Callback to request switching back to Home Space mode.
 */
@SuppressLint("RestrictedApi")
@Composable
fun MySpatialContent(
    viewModel: SolarSystemViewModel,
    onRequestHomeSpaceMode: () -> Unit
) {
    var isPlanetListExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var measuredHeight by remember { mutableStateOf(400.dp) }
    
    SpatialPanel(
        // Dynamic height based on measured content
        modifier = SubspaceModifier.offset(x = 500.dp, z = (-200).dp)
            .width(320.dp)
            .height(measuredHeight),
        dragPolicy = MovePolicy(),
        resizePolicy = ResizePolicy()
    ) {
        Surface(
            modifier = Modifier
                .wrapContentHeight(unbounded = true, align = Alignment.Top)
                .onSizeChanged { size ->
                    measuredHeight = with(density) { size.height.toDp() }
                }
        ) {
            Dashboard(
                viewModel = viewModel,
                isExpanded = isPlanetListExpanded,
                onToggleExpand = { isPlanetListExpanded = !isPlanetListExpanded },
                modifier = Modifier
                    .width(320.dp) // Maintain fixed width
                    .padding(24.dp)
            )
        }
        Orbiter(
            position = ContentEdge.Top,
            offset = 20.dp,
            alignment = Alignment.End,
            shape = SpatialRoundedCornerShape(CornerSize(28.dp))
        ) {
            HomeSpaceModeIconButton(
                onClick = onRequestHomeSpaceMode,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

/**
 * Container for UI elements when the app is running in standard 2D mode.
 *
 * @param viewModel The shared Solar System view model.
 * @param onRequestFullSpaceMode Callback to request switching to Full Space (XR) mode.
 */
@SuppressLint("RestrictedApi")
@Composable
fun My2DContent(
    viewModel: SolarSystemViewModel,
    onRequestFullSpaceMode: () -> Unit
) {
    Surface {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Dashboard(
                viewModel = viewModel,
                isExpanded = true,
                onToggleExpand = { /* No-op in 2D mode or implement local state if needed */ },
                modifier = Modifier.weight(1f).padding(48.dp)
            )
            
            // Preview does not current support XR sessions.
            if (!LocalInspectionMode.current && LocalSession.current != null) {
                FullSpaceModeIconButton(
                    onClick = onRequestFullSpaceMode,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
    }
}

/**
 * High-level Dashboard component that connects the UI state to the ViewModel.
 */
@Composable
fun Dashboard(
    viewModel: SolarSystemViewModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        uiState = uiState,
        onTogglePause = { viewModel.togglePause() },
        onSetSpeed = { viewModel.setSpeed(it) },
        onSetScale = { viewModel.setScale(it) },
        onToggleSkybox = { viewModel.toggleSkybox() },
        onSelectPlanet = { viewModel.selectPlanet(it) },
        onAdvanceState = { viewModel.advanceStartupState() },
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        modifier = modifier
    )
}

@Composable
fun DashboardContent(
    uiState: SolarSystemUiState,
    onTogglePause: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetScale: (Float) -> Unit,
    onToggleSkybox: () -> Unit,
    onSelectPlanet: (Planet) -> Unit,
    onAdvanceState: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    planets: List<Planet> = SolarSystemRepository.allBodies
) {
    // Startup sequence is now handled by StartupText3D in Subspace
    
    Column(
        modifier = modifier
    ) {
            Text("Pocket Orrery 🌎🔭", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
    
            MediaControls(
                isPaused = uiState.isPaused,
                currentSpeed = uiState.simulationSpeed,
                onTogglePause = onTogglePause,
                onSetSpeed = onSetSpeed
            )

        Spacer(modifier = Modifier.height(16.dp))

        Text("System Scale: ${String.format("%.1f", uiState.scale)}x", style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Slider(
            value = uiState.scale,
            onValueChange = onSetScale,
            valueRange = 0.5f..5.0f,
            steps = 8
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Galaxy Environment", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = uiState.isSkyboxEnabled,
                onCheckedChange = { onToggleSkybox() }
            )
        }

        // Show selected planet info OR instructions
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Planets", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
             Column {
                // Show selected planet info OR instructions
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (uiState.selectedPlanet != null) {
                            Text(uiState.selectedPlanet!!.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(uiState.selectedPlanet!!.description, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Select a Planet", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("For more details.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    planets.forEach { planet ->
                        PlanetItem(
                            planet = planet,
                            isSelected = uiState.selectedPlanet == planet,
                            onClick = { onSelectPlanet(planet) }
                        )
                    }
                }
             }
        }
    }
}


@Composable
fun PlanetItem(planet: Planet, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(planet.color, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(planet.name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}


@Composable
fun MediaControls(
    isPaused: Boolean,
    currentSpeed: Float,
    onTogglePause: () -> Unit,
    onSetSpeed: (Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Speed: ${String.format("%.1fx", currentSpeed)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Fast Rewind (Subtract 1x from speed)
            IconButton(onClick = { onSetSpeed(currentSpeed - 1.0f) }) {
                Icon(Icons.Default.FastRewind, contentDescription = "Fast Rewind")
            }
            
            // Rewind / Reverse (Reset to -1x)
            IconButton(onClick = { onSetSpeed(-1.0f) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Reverse")
            }
            
            // Play / Pause
            Button(
                onClick = onTogglePause,
                 // Highlight play button if paused, or if playing (to show it's active)
                 // Let's just use standard button style
            ) {
                 Icon(
                     if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                     contentDescription = if (isPaused) "Play" else "Pause"
                 )
            }

            // Forward (Reset to 1x)
            IconButton(onClick = { onSetSpeed(1.0f) }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
            }

            // Fast Forward (Add 1x to speed)
            IconButton(onClick = { onSetSpeed(currentSpeed + 1.0f) }) {
                Icon(Icons.Default.FastForward, contentDescription = "Fast Forward")
            }
        }
    }
}



@Composable
fun FullSpaceModeIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(id = R.drawable.ic_full_space_mode_switch),
            contentDescription = stringResource(R.string.switch_to_full_space_mode)
        )
    }
}

@Composable
fun HomeSpaceModeIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(id = R.drawable.ic_home_space_mode_switch),
            contentDescription = stringResource(R.string.switch_to_home_space_mode)
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 600)
@Composable
private fun DashboardPreview() {
    PocketOrreryTheme {
        Surface {
            DashboardContent(
                uiState = SolarSystemUiState(
                    scale = 1.0f,
                    selectedPlanet = SolarSystemRepository.planets.find { it.name == "Earth" }
                ),
                onTogglePause = {},
                onSetSpeed = {},
                onSetScale = {},
                onToggleSkybox = {},
                onSelectPlanet = {},
                onAdvanceState = {},
                isExpanded = true,
                onToggleExpand = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlanetItemPreview() {
    PocketOrreryTheme {
        PlanetItem(
            planet = SolarSystemRepository.planets[2], // Earth
            isSelected = true,
            onClick = {}
        )
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun StartupText3D(
    viewModel: SolarSystemViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    if (uiState.startupState != StartupState.Finished && uiState.startupState != StartupState.Reveal) {
         SpatialPanel(
            modifier = SubspaceModifier.offset(z = -1.2f.dp, y = 0.0f.dp).width(800.dp).height(400.dp),
            dragPolicy = MovePolicy(),
            resizePolicy = ResizePolicy()
        ) {
             // Transparent surface for floating text effect
             Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.startupState == StartupState.Welcome,
                        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(1000)),
                        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(1000))
                    ) {
                        Text(
                            "Welcome to the Future of Computing",
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.startupState == StartupState.Author,
                        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(1000)),
                        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(1000))
                    ) {
                        Text(
                            "PocketOrreryXR\nby Saumil Shah",
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
             }
        }
    }

    // Drive the sequence - runs independently of UI visibility
    LaunchedEffect(uiState.startupState) {
        when (uiState.startupState) {
            StartupState.Welcome, StartupState.Author -> {
                delay(3000L)
                viewModel.advanceStartupState()
            }
            StartupState.Reveal -> {
                delay(3500L) // Wait for the 3s reveal animation + buffer
                viewModel.advanceStartupState()
            }
            else -> {}
        }
    }
}