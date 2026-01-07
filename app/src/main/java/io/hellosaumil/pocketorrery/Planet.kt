package io.hellosaumil.pocketorrery

import androidx.compose.ui.graphics.Color

/**
 * Data model representing a celestial body in the solar system.
 *
 * @property name Display name of the body.
 * @property radius Relative physical size for visual representation.
 * @property orbitDistance Relative distance from the center (Sun).
 * @property orbitSpeed Relative speed of rotation around the center.
 * @property color Identifying color used in UI elements.
 * @property description Scientific description or interesting fact.
 */
data class Planet(
    val name: String,
    val radius: Float, // Relative size
    val orbitDistance: Float, // Relative distance from sun
    val orbitSpeed: Float, // Relative speed (degrees per frame or second)
    val rotationSpeed: Float, // Rotation speed around axis (degrees per second)
    val color: Color,
    val description: String
)
