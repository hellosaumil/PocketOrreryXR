package io.hellosaumil.pocketorrery

import androidx.compose.ui.graphics.Color

object SolarSystemRepository {
    val sol = Planet(
        name = "Sun",
        radius = 1.0f,
        orbitDistance = 0f,
        orbitSpeed = 0f,
        color = Color(0xFFFFD700),
        description = "The Star at the center of our Solar System."
    )

    val planets = listOf(
        Planet("Mercury", 0.1f, 1.5f, 4.0f, Color.Gray, "The smallest planet."),
        Planet("Venus", 0.2f, 2.0f, 3.0f, Color(0xFFFFC107), "Spinning backwards."),
        Planet("Earth", 0.2f, 2.8f, 2.5f, Color.Blue, "Our home."),
        Planet("Mars", 0.15f, 3.5f, 2.0f, Color.Red, "The Red Planet."),
        Planet("Jupiter", 0.6f, 5.5f, 1.0f, Color(0xFFDEB887), "Gas Giant."),
        Planet("Saturn", 0.5f, 7.5f, 0.8f, Color(0xFFF4A460), "Has rings."),
        Planet("Uranus", 0.4f, 9.5f, 0.6f, Color(0xFFADD8E6), "Ice Giant."),
        Planet("Neptune", 0.4f, 11.0f, 0.5f, Color.Blue, "Windy.")
    )
    
    val allBodies = listOf(sol) + planets
}
