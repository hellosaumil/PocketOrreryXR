package io.hellosaumil.pocketorrery

import androidx.compose.ui.graphics.Color

data class Planet(
    val name: String,
    val radius: Float, // Relative size
    val orbitDistance: Float, // Relative distance from sun
    val orbitSpeed: Float, // Relative speed (degrees per frame or second)
    val color: Color,
    val description: String
)
