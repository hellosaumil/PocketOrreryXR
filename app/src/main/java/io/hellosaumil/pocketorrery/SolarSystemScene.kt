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
            
            try {
                // Create Sun entity - acting as the Root for the system
                // Start 1.5m in front of the user
                // Base Scale of Sun is 0.2
                val sunEntity = GltfModelEntity.create(
                    session,
                    model,
                    Pose(translation = Vector3(0f, 0f, -1.5f))
                ).apply {
                    setScale(0.2f) // 20cm diameter for sun
                    
                    // Make sun draggable to move the entire solar system
                    // scaleInZ=false prevents uncontrolled scaling. Use Slider instead.
                    val movableComponent = MovableComponent.createSystemMovable(session, scaleInZ = false)
                    addComponent(movableComponent)
                }
                newEntities[SolarSystemRepository.sol] = sunEntity
                
                android.util.Log.d("SolarSystemScene", "Sun entity created with MovableComponent (Root)")
                
                // Create planet entities
                SolarSystemRepository.planets.forEach { planet ->
                    try {
                        // Compensate for Sun's scale (0.2)
                        // Sun Scale = 0.2. Child Pos = World Pos / 0.2.
                        val initialX = (planet.orbitDistance * 0.1f) / 0.2f
                        
                        val entity = GltfModelEntity.create(
                            session,
                            model,
                            Pose(translation = Vector3(initialX, 0f, 0f))
                        ).apply {
                            // Parent to Sun so they move with it
                            sunEntity.addChild(this)
                            
                            // Scale planets: 2cm base + up to 9cm based on radius
                            // Compensate for Sun's scale (0.2) so their size is preserved in world space
                            setScale((0.02f + planet.radius * 0.15f) / 0.2f)
                        }
                        newEntities[planet] = entity
                    } catch (e: Exception) {
                        android.util.Log.e("SolarSystemScene", "Failed to create entity for ${planet.name}", e)
                    }
                }
                
                entities = newEntities
                
            } catch (e: Exception) {
                android.util.Log.e("SolarSystemScene", "Failed to create sun entity", e)
            }
        }
        
        onDispose {
            entities.values.forEach { it.dispose() }
        }
    }
    
    // Handle selected planet highlighting and system scaling
    LaunchedEffect(uiState.selectedPlanet, uiState.scale, entities) {
        entities.forEach { (planet, entity) ->
            val isSelected = uiState.selectedPlanet == planet
            val globalScale = uiState.scale
            
            val scale = if (planet == SolarSystemRepository.sol) {
                // Sun: Apply global scale. No selection multiplier.
                // Base 0.2 scales with globalScale.
                0.2f * globalScale
            } else {
                // Planets: Compensate for Sun's base scale (0.2).
                // Global scale is inherited from parent (Sun).
                // So we only set the LOCAL scale relative to the Sun's 0.2 factor.
                val base = (0.02f + planet.radius * 0.15f) / 0.2f
                if (isSelected) base * 1.5f else base
            }
            
            entity.setScale(scale)
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
                
                // Update planet positions (orbiting around Sun (0,0,0) local space)
                entities.forEach { (planet, entity) ->
                    // Sun doesn't orbit itself
                    if (planet == SolarSystemRepository.sol) {
                        return@forEach
                    }
                    
                    val angle = animationTime * planet.orbitSpeed * 0.3f
                    // Compensate for Sun's scale (0.2)
                    val distance = (planet.orbitDistance * 0.1f) / 0.2f
                    
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
