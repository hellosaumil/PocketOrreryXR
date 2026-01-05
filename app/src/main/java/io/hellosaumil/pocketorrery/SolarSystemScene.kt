package io.hellosaumil.pocketorrery

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.xr.compose.platform.LocalSession
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import kotlin.io.path.Path
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("RestrictedApi")
@Composable
fun SolarSystemScene(viewModel: SolarSystemViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session = LocalSession.current
    
    if (session == null) {
        // Fallback for Preview
        androidx.compose.material3.Text(
            text = "3D Solar System Scene\n(Not available in standard Preview)",
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )
        return
    }

    // Animation time state
    var animationTime by remember { mutableFloatStateOf(0f) }

    // Smooth scaling
    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = uiState.scale,
        label = "scaleAnimation"
    )

    // Startup Reveal Animation
    val startupScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (uiState.startupState == StartupState.Reveal || uiState.startupState == StartupState.Finished) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 3000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "startupReveal"
    )
    
    // State for loaded models
    var models by remember { mutableStateOf<Map<Planet, GltfModel>>(emptyMap()) }
    var entities by remember { mutableStateOf<Map<Planet, GltfModelEntity>>(emptyMap()) }
    
    // Load models asynchronously
    LaunchedEffect(session) {
        val loadedModels = mutableMapOf<Planet, GltfModel>()
        SolarSystemRepository.allBodies.forEach { planet ->
             try {
                // "Sun ☀️" -> "sun". "Earth 🌎" -> "earth"
                val name = planet.name.lowercase().split(" ")[0]
                val filename = if (name == "sun") "sun.gltf" else "$name.gltf"
                
                val model = GltfModel.create(
                    session,
                    Path("models/$filename")
                )
                loadedModels[planet] = model
                android.util.Log.d("SolarSystemScene", "Loaded $filename for ${planet.name}")
            } catch (e: Exception) {
                android.util.Log.e("SolarSystemScene", "Failed to load model for ${planet.name}", e)
            }
        }
        models = loadedModels
    }
    
    // Create entities when models are loaded
    DisposableEffect(models) {
        if (models.isNotEmpty()) {
            val newEntities = mutableMapOf<Planet, GltfModelEntity>()
            
            // 1. Create Sun first (Root)
            val sunModel = models[SolarSystemRepository.sol]
            var sunEntity: GltfModelEntity? = null
            
            if (sunModel != null) {
                try {
                    // Start 1.0m in front of the user
                    sunEntity = GltfModelEntity.create(
                        session,
                        sunModel,
                        Pose(translation = Vector3(0f, 0f, -0.5f))
                    ).apply {
                        setScale(0f) // Start invisible, will scale up via animation
                        val movableComponent = MovableComponent.createSystemMovable(session, scaleInZ = false)
                        addComponent(movableComponent)
                    }
                    newEntities[SolarSystemRepository.sol] = sunEntity!!
                    android.util.Log.d("SolarSystemScene", "Sun entity created")
                } catch (e: Exception) {
                    android.util.Log.e("SolarSystemScene", "Failed to create Sun entity", e)
                }
            }
            
            // 2. Create other planets
            val rootEntity = sunEntity // Parent everything to Sun
            if (rootEntity != null) {
                SolarSystemRepository.planets.forEach { planet ->
                    val model = models[planet] ?: return@forEach
                    try {
                        val initialX = (planet.orbitDistance * 0.1f) / 0.2f
                        
                        val entity = GltfModelEntity.create(
                            session,
                            model,
                            Pose(translation = Vector3(initialX, 0f, 0f))
                        ).apply {
                            rootEntity.addChild(this)
                            setScale(0f) // Start invisible, will scale up via animation
                        }
                        newEntities[planet] = entity
                    } catch (e: Exception) {
                        android.util.Log.e("SolarSystemScene", "Failed to create entity for ${planet.name}", e)
                    }
                }
            }
            entities = newEntities
        }
        
        onDispose {
            entities.values.forEach { it.dispose() }
        }
    }
    
    // Handle selected planet highlighting and system scaling
    // This effect now frame-locks the scale to prevent uncontrolled pinch-scaling
    LaunchedEffect(uiState.selectedPlanet, entities) {
        if (entities.isEmpty()) return@LaunchedEffect
        
        while(true) {
            withFrameMillis { } // Sync with frame
            
            // Effective scale combines user setting and startup animation
            val globalScale = animatedScale * startupScale
            
            // Determine Sun Swell Factor (1.5 if selected)
            val isSunSelected = uiState.selectedPlanet == SolarSystemRepository.sol
            val sunSwellFactor = if (isSunSelected) 1.5f else 1.0f
            
            entities.forEach { (planet, entity) ->
                val isSelected = uiState.selectedPlanet == planet
                
                val targetScale = if (planet == SolarSystemRepository.sol) {
                    // Sun: Apply global scale, AND selection multiplier if selected.
                    val base = 0.2f * globalScale
                    base * sunSwellFactor
                } else {
                    // Planets:
                    // 1. Calculate Base Size (unchanged)
                    val baseSize = (0.02f + planet.radius * 0.15f) / 0.2f
                    
                    // 2. Counter-Scale: Divide by sunSwellFactor to cancel out parent scaling
                    val counterScaledSize = baseSize / sunSwellFactor
                    
                    // 3. Apply Selection: Multiply by 1.5 if selected
                    if (isSelected) counterScaledSize * 1.5f else counterScaledSize
                }
                
                // Enforce scale precisely to override any component-level scaling
                entity.setScale(targetScale)
            }
        }
    }
    
    // Animation Loop
    LaunchedEffect(uiState.isPaused, entities) {
        if (entities.isEmpty()) return@LaunchedEffect
        
        val sessionStartTime = System.nanoTime()
        val baseTime = animationTime
        
        while (!uiState.isPaused) {
            withFrameMillis { _ ->
                val elapsedTime = (System.nanoTime() - sessionStartTime) / 1_000_000_000f
                animationTime = baseTime + elapsedTime
                
                // Determine Sun Swell for Position Counter-Scaling
                val isSunSelected = uiState.selectedPlanet == SolarSystemRepository.sol
                val sunSwellFactor = if (isSunSelected) 1.5f else 1.0f
                
                // Update planet positions (orbiting around Sun (0,0,0) local space)
                entities.forEach { (planet, entity) ->
                    // Sun doesn't orbit itself
                    if (planet == SolarSystemRepository.sol) {
                        return@forEach
                    }
                    
                    val angle = animationTime * planet.orbitSpeed * 0.3f
                    // Compensate for Sun's scale (0.2) AND Sun's Swell Factor
                    val distance = ((planet.orbitDistance * 0.1f) / 0.2f) / sunSwellFactor
                    
                    val x = cos(angle) * distance
                    val z = sin(angle) * distance
                    
                    entity.setPose(
                        Pose(translation = Vector3(x, 0f, z))
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SolarSystemScenePreview() {
    io.hellosaumil.pocketorrery.ui.theme.PocketOrreryTheme {
        // We can't truly preview the ViewModel-driven scene without mocking the VM,
        // but since we return early when session is null, an empty VM or mock is fine just to show the placeholder.
        // For simplicity in this preview, we just call it. The ViewModel creation might effectively be "empty" or throw if not handled carefully,
        // but normally viewModel() works in preview if it has a no-arg constructor or we use valid composition locals.
        // SolarSystemViewModel has a default constructor so it should work.
        SolarSystemScene(viewModel = androidx.lifecycle.viewmodel.compose.viewModel())
    }
}
