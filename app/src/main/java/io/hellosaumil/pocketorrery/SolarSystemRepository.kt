package io.hellosaumil.pocketorrery

import androidx.compose.ui.graphics.Color

/**
 * Repository containing the data definitions for all celestial bodies.
 * 
 * In a more complex app, this could fetch data from an API or database.
 * For Pocket Orrery, we use a static list of the eight planets and the Sun.
 */
object SolarSystemRepository {
    val sol = Planet(
        name = "Sun ☀️",
        radius = 1.0f,
        orbitDistance = 0f,
        orbitSpeed = 0f,
        rotationSpeed = 2.0f, // Sun rotates slowly
        axialTilt = 0.0f, // Reset tilt to avoid visual confusion
        color = Color(0xFFFFD700),
        description = "The Star at the center of our Solar System."
    )

    val planets = listOf(
        Planet("Mercury", 0.1f, 1.5f, 4.0f, 5.0f, 0.0f, Color.Gray, "The smallest planet."), // Slow rotation, negligible tilt
        Planet("Venus", 0.2f, 2.0f, 3.0f, -15.0f, 0.0f, Color(0xFFFFC107), "Spinning backwards."), // Retrograde, faster visible rotation
        Planet("Earth 🌎", 0.2f, 2.8f, 2.5f, 20.0f, 0.0f, Color.Blue, "Our home."), // Baseline
        Planet("Mars", 0.15f, 3.5f, 2.0f, 19.0f, 0.0f, Color.Red, "The Red Planet."), // Similar to Earth
        Planet("Jupiter", 0.6f, 5.5f, 1.0f, 45.0f, 0.0f, Color(0xFFDEB887), "Gas Giant."), // Very fast, small tilt
        Planet("Saturn", 0.5f, 7.5f, 0.8f, 40.0f, 0.0f, Color(0xFFF4A460), "Has rings."), // Fast, distinct tilt
        Planet("Uranus", 0.4f, 9.5f, 0.6f, -30.0f, 97.8f, Color(0xFFADD8E6), "Ice Giant."), // Retrograde, rolling on side - KEPT RELEVANT
        Planet("Neptune", 0.4f, 11.0f, 0.5f, 35.0f, 0.0f, Color.Blue, "Windy.") // Fast, distinct tilt
    )
    
    val allBodies = listOf(sol) + planets
}
