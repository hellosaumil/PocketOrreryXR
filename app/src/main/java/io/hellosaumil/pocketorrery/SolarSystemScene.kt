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
            
            // Create Sun entity
            try {
                val sunEntity = GltfModelEntity.create(
                    session,
                    model,
                    Pose(translation = Vector3(0f, 0f, -1f)) // 1 meter in front
                ).apply {
                    setScale(0.3f) // 30cm diameter
                }
                newEntities[SolarSystemRepository.sol] = sunEntity
                android.util.Log.d("SolarSystemScene", "Sun entity created with scale 0.3")
            } catch (e: Exception) {
                android.util.Log.e("SolarSystemScene", "Failed to create sun entity", e)
            }
            
            // Create planet entities
            SolarSystemRepository.planets.forEach { planet ->
                try {
                    val entity = GltfModelEntity.create(
                        session,
                        model,
                        Pose(translation = Vector3(0f, 0f, -1f)) // Start 1 meter in front
                    ).apply {
                        setScale(planet.radius * 0.1f) // Scale based on planet size
                    }
                    newEntities[planet] = entity
                    android.util.Log.d("SolarSystemScene", "Planet ${planet.name} entity created with scale ${planet.radius * 0.1f}")
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
    
    // Animation Loop
    LaunchedEffect(uiState.isPaused, entities) {
        if (entities.isEmpty()) return@LaunchedEffect
        
        val startTime = System.nanoTime()
        while (!uiState.isPaused) {
            withFrameMillis { _ ->
                animationTime = (System.nanoTime() - startTime) / 1_000_000_000f
                
                // Update planet positions (in meters, 1 meter in front of user)
                entities.forEach { (planet, entity) ->
                    val angle = animationTime * planet.orbitSpeed * 0.5f
                    val distance = planet.orbitDistance * 0.15f // Scale orbit to reasonable size
                    val x = cos(angle) * distance
                    val z = -1f + sin(angle) * distance // Center 1m in front
                    
                    entity.setPose(
                        Pose(translation = Vector3(x, 0f, z))
                    )
                }
            }
        }
    }
}
