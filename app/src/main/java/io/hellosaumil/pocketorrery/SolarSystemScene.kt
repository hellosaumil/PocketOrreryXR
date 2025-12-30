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
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
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
    
    // State for tracking dragging
    var isDraggingSun by remember { mutableStateOf(false) }
    
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
            
            // Create Sun entity - stationary at center, but draggable
            try {
                val sunEntity = GltfModelEntity.create(
                    session,
                    model,
                    Pose(translation = Vector3(0f, 0f, -1.5f)) // 1.5 meters in front, centered
                ).apply {
                    setScale(0.2f) // 20cm diameter for sun
                    
                    // Make sun draggable to move the solar system
                    val movableComponent = MovableComponent.createSystemMovable(session, scaleInZ = false)
                    
                    // Add listener to pause during drag
                    movableComponent.addMoveListener(object : EntityMoveListener {
                        override fun onMoveStart(
                            entity: Entity,
                            initialInputRay: Ray,
                            initialPose: Pose,
                            initialScale: Float,
                            initialParent: Entity
                        ) {
                            isDraggingSun = true
                        }
                        
                        override fun onMoveEnd(
                            entity: Entity,
                            finalInputRay: Ray,
                            finalPose: Pose,
                            finalScale: Float,
                            updatedParent: Entity?
                        ) {
                            isDraggingSun = false
                        }
                    })
                    
                    addComponent(movableComponent)
                }
                newEntities[SolarSystemRepository.sol] = sunEntity
                android.util.Log.d("SolarSystemScene", "Sun entity created with MovableComponent")
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
                        // Scale planets: 2cm base + up to 9cm based on radius
                        setScale(0.02f + planet.radius * 0.15f)
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
            val baseScale = if (planet == SolarSystemRepository.sol) 0.2f else 0.02f + planet.radius * 0.15f
            val scale = if (isSelected) baseScale * 1.5f else baseScale
            entity.setScale(scale)
        }
    }
    
    // Animation Loop - only animate planets, not the sun
    // Pauses when user is dragging the sun or when paused via UI
    LaunchedEffect(uiState.isPaused, isDraggingSun, entities) {
        if (entities.isEmpty()) return@LaunchedEffect
        
        // Record when this animation session started, offset by previous accumulated time
        val sessionStartTime = System.nanoTime()
        val baseTime = animationTime // Start from current animation time
        val sunEntity = entities[SolarSystemRepository.sol]
        
        while (!uiState.isPaused && !isDraggingSun) {
            withFrameMillis { _ ->
                // Add elapsed time to the base time (preserves position across pauses)
                val elapsedTime = (System.nanoTime() - sessionStartTime) / 1_000_000_000f
                animationTime = baseTime + elapsedTime
                
                // Get sun's current position (may have been moved by user)
                val sunPos = sunEntity?.getPose()?.translation ?: Vector3(0f, 0f, -1.5f)
                
                // Update planet positions (orbiting around sun's current position)
                entities.forEach { (planet, entity) ->
                    // Sun stays where user drags it
                    if (planet == SolarSystemRepository.sol) {
                        return@forEach
                    }
                    
                    val angle = animationTime * planet.orbitSpeed * 0.3f
                    val distance = planet.orbitDistance * 0.1f // 10cm per unit
                    val x = sunPos.x + cos(angle) * distance
                    val z = sunPos.z + sin(angle) * distance
                    
                    entity.setPose(
                        Pose(translation = Vector3(x, sunPos.y, z))
                    )
                }
            }
        }
    }
}
