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
import androidx.xr.runtime.math.Quaternion
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

/**
 * The main 3D scene for the Solar System.
 *
 * This component manages the loading of 3D models, creation of entities, 
 * and handled the animation loop for planetary orbits. It uses the Jetpack XR SDK
 * to render high-fidelity 3D content in a spatial environment.
 *
 * @param viewModel The view model containing the solar system state.
 */
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

    // Individual selection animations for smooth scaling transitions
    val selectionFactors = SolarSystemRepository.allBodies.associateWith { planet ->
        androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (uiState.selectedPlanet == planet) 1.5f else 1.0f,
            label = "${planet.name}Swell",
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        )
    }
    
    // State for loaded models
    var models by remember { mutableStateOf<Map<Planet, GltfModel>>(emptyMap()) }
    var entities by remember { mutableStateOf<Map<Planet, GltfModelEntity>>(emptyMap()) }
    
    // Skybox State
    var skyboxModel by remember { mutableStateOf<GltfModel?>(null) }
    var skyboxEntity by remember { mutableStateOf<GltfModelEntity?>(null) }
    
    // Load models asynchronously
    LaunchedEffect(session) {
        val loadedModels = mutableMapOf<Planet, GltfModel>()
        
        // Load Skybox
        try {
            val skybox = GltfModel.create(session, Path("models/milky_way.gltf"))
            skyboxModel = skybox
            android.util.Log.d("SolarSystemScene", "Loaded milky_way.gltf")
        } catch (e: Exception) {
            android.util.Log.e("SolarSystemScene", "Failed to load skybox", e)
        }

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
    
    // Create Skybox Entity - Controlled by Toggle
    DisposableEffect(skyboxModel, uiState.isSkyboxEnabled) {
        if (skyboxModel != null && uiState.isSkyboxEnabled) {
            try {
                // Background at 0,0,0, not movable, large scale handled by model (radius 50 m)
                // Inverted faces allow viewing from inside.
                skyboxEntity = GltfModelEntity.create(
                    session,
                    skyboxModel!!,
                    Pose(translation = Vector3(0f, 0f, 0f))
                )
                android.util.Log.d("SolarSystemScene", "Skybox entity created")
            } catch (e: Exception) {
                android.util.Log.e("SolarSystemScene", "Failed to create Skybox entity", e)
            }
        }
        
        onDispose {
            skyboxEntity?.dispose()
            skyboxEntity = null
        }
    }

    // Trigger welcome sequence once skybox is loaded
    LaunchedEffect(skyboxEntity) {
        if (skyboxEntity != null && uiState.startupState == StartupState.Loading) {
            viewModel.advanceStartupState()
        }
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
    
    // Unified Animation & Scaling Loop
    // This merged loop ensures that scaling and orbit positions are updated in the same frame,
    // avoiding the one-frame delay jitter when the Sun's scale changes and planets need to counter-scale.
    LaunchedEffect(entities, uiState.isPaused) {
        if (entities.isEmpty()) return@LaunchedEffect
        
        val sessionStartTime = System.nanoTime()
        val startAnimationTime = animationTime
        
        while(true) {
            withFrameMillis { _ ->
                // 1. Update Animation Time if not paused
                if (!uiState.isPaused) {
                    val elapsedTime = (System.nanoTime() - sessionStartTime) / 1_000_000_000f
                    animationTime = startAnimationTime + elapsedTime
                }
                
                // 2. Synchronized Transformation Update
                val globalScale = animatedScale * startupScale
                val sunSwellFactor = selectionFactors[SolarSystemRepository.sol]?.value ?: 1.0f
                
                entities.forEach { (planet, entity) ->
                    // --- Calculate Scale ---
                    val selectionFactor = selectionFactors[planet]?.value ?: 1.0f
                    val targetScale = if (planet == SolarSystemRepository.sol) {
                        0.2f * globalScale * sunSwellFactor
                    } else {
                        val baseSize = (0.02f + planet.radius * 0.15f) / 0.2f
                        (baseSize / sunSwellFactor) * selectionFactor
                    }
                    entity.setScale(targetScale)
                    
                    if (planet != SolarSystemRepository.sol) {
                        entity.setPose(calculatePlanetPose(planet, animationTime, sunSwellFactor))
                    }
                    }
                }
            }
        }
    }


private fun calculatePlanetPose(planet: Planet, animationTime: Float, sunSwellFactor: Float): Pose {
    // 1. Orbit Position
    val orbitAngle = animationTime * planet.orbitSpeed * 0.3f
    // Counter-scale the orbit distance as well to keep planets from jumping when Sun is selected
    val distance = ((planet.orbitDistance * 0.1f) / 0.2f) / sunSwellFactor

    val x = cos(orbitAngle) * distance
    val z = sin(orbitAngle) * distance

    // 2. Axial Rotation (Spin)
    val spinAngle = (animationTime * planet.rotationSpeed) % 360f
    // Vector3.Up is (0, 1, 0)
    val rotation = Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), spinAngle)

    return Pose(translation = Vector3(x, 0f, z), rotation = rotation)
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
