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
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import kotlin.io.path.Path
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("RestrictedApi")
@Composable
fun SolarSystemScene(viewModel: SolarSystemViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session = LocalSession.current ?: return
    
    // Animation time state
    var animationTime by remember { mutableFloatStateOf(0f) }
    
    // State for loaded model
    var sphereModel by remember { mutableStateOf<GltfModel?>(null) }
    var entities by remember { mutableStateOf<Map<Planet, GltfModelEntity>>(emptyMap()) }
    
    // Load the sphere model asynchronously
    LaunchedEffect(session) {
        try {
            sphereModel = GltfModel.create(
                session,
                Path("models/sphere.glb")
            )
            android.util.Log.d("SolarSystemScene", "Sphere model loaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("SolarSystemScene", "Failed to load sphere model", e)
        }
    }
    
    // Create entities when model is loaded
    DisposableEffect(sphereModel) {
        val model = sphereModel
        if (model != null) {
            val newEntities = mutableMapOf<Planet, GltfModelEntity>()
            
            // Create Sun entity - stationary at center
            try {
                val sunEntity = GltfModelEntity.create(
                    session,
                    model,
                    Pose(translation = Vector3(0f, 0f, -1.5f)) // 1.5 meters in front, centered
                ).apply {
                    setScale(0.15f) // 15cm diameter for sun
                }
                newEntities[SolarSystemRepository.sol] = sunEntity
                android.util.Log.d("SolarSystemScene", "Sun entity created")
            } catch (e: Exception) {
                android.util.Log.e("SolarSystemScene", "Failed to create sun entity", e)
            }
            
            // Create planet entities - start at their orbital positions
            SolarSystemRepository.planets.forEach { planet ->
                try {
                    val initialX = planet.orbitDistance * 0.1f
                    val entity = GltfModelEntity.create(
                        session,
                        model,
                        Pose(translation = Vector3(initialX, 0f, -1.5f))
                    ).apply {
                        // Scale planets relative to their size (smaller than sun)
                        setScale(0.01f + planet.radius * 0.02f)
                    }
                    newEntities[planet] = entity
                    android.util.Log.d("SolarSystemScene", "Planet ${planet.name} entity created")
                } catch (e: Exception) {
                    android.util.Log.e("SolarSystemScene", "Failed to create entity for ${planet.name}", e)
                }
            }
            
            entities = newEntities
        }
        
        onDispose {
            entities.values.forEach { it.dispose() }
        }
    }
    
    // Handle selected planet highlighting
    LaunchedEffect(uiState.selectedPlanet, entities) {
        entities.forEach { (planet, entity) ->
            val isSelected = uiState.selectedPlanet == planet
            // Scale up selected planet
            val baseScale = if (planet == SolarSystemRepository.sol) 0.15f else 0.01f + planet.radius * 0.02f
            val scale = if (isSelected) baseScale * 1.5f else baseScale
            entity.setScale(scale)
        }
    }
    
    // Animation Loop - only animate planets, not the sun
    LaunchedEffect(uiState.isPaused, entities) {
        if (entities.isEmpty()) return@LaunchedEffect
        
        val startTime = System.nanoTime()
        while (!uiState.isPaused) {
            withFrameMillis { _ ->
                animationTime = (System.nanoTime() - startTime) / 1_000_000_000f
                
                // Update planet positions (orbiting around sun)
                entities.forEach { (planet, entity) ->
                    // Sun stays stationary
                    if (planet == SolarSystemRepository.sol) {
                        return@forEach
                    }
                    
                    val angle = animationTime * planet.orbitSpeed * 0.3f
                    val distance = planet.orbitDistance * 0.1f // 10cm per unit
                    val x = cos(angle) * distance
                    val z = -1.5f + sin(angle) * distance // Orbit around sun at z=-1.5
                    
                    entity.setPose(
                        Pose(translation = Vector3(x, 0f, z))
                    )
                }
            }
        }
    }
}
