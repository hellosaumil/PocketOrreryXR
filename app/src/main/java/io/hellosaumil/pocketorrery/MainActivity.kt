package io.hellosaumil.pocketorrery

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
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
import io.hellosaumil.pocketorrery.ui.theme.PocketOrreryTheme

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
                        
                        // The 2D Control Panel
                        MySpatialContent(
                            viewModel = viewModel,
                            onRequestHomeSpaceMode = spatialConfiguration::requestHomeSpaceMode
                        )
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

@SuppressLint("RestrictedApi")
@Composable
fun MySpatialContent(
    viewModel: SolarSystemViewModel,
    onRequestHomeSpaceMode: () -> Unit
) {
    SpatialPanel(
        modifier = SubspaceModifier.width(320.dp).height(700.dp),
        dragPolicy = MovePolicy(),
        resizePolicy = ResizePolicy()
    ) {
        Surface {
            Dashboard(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
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
            Dashboard(viewModel = viewModel, modifier = Modifier.weight(1f).padding(48.dp))
            
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

@Composable
fun Dashboard(viewModel: SolarSystemViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        uiState = uiState,
        onTogglePause = { viewModel.togglePause() },
        onSetScale = { viewModel.setScale(it) },
        onSelectPlanet = { viewModel.selectPlanet(it) },
        modifier = modifier
    )
}

@Composable
fun DashboardContent(
    uiState: SolarSystemUiState,
    onTogglePause: () -> Unit,
    onSetScale: (Float) -> Unit,
    onSelectPlanet: (Planet) -> Unit,
    modifier: Modifier = Modifier,
    planets: List<Planet> = SolarSystemRepository.allBodies
) {
    Column(modifier = modifier) {
        Text("Pocket Orrery 🌎🔭", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onTogglePause) {
            Text(if (uiState.isPaused) "Resume Orbit" else "Pause Orbit")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("System Scale: ${String.format("%.1f", uiState.scale)}x", style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Slider(
            value = uiState.scale,
            onValueChange = onSetScale,
            valueRange = 0.5f..5.0f
        )

        // Show selected planet info
        uiState.selectedPlanet?.let { planet ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(planet.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(planet.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Planets", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(planets) { planet ->
                PlanetItem(
                    planet = planet,
                    isSelected = uiState.selectedPlanet == planet,
                    onClick = { onSelectPlanet(planet) }
                )
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
                onSetScale = {},
                onSelectPlanet = {},
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