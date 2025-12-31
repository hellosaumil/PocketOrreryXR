package io.hellosaumil.pocketorrery.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun PocketOrreryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Preview(name = "Light Theme")
@Preview(name = "Dark Theme", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ThemePreview() {
    PocketOrreryTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Theme Preview", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    ColorBox(name = "Primary", color = MaterialTheme.colorScheme.primary, onColor = MaterialTheme.colorScheme.onPrimary)
                    ColorBox(name = "Secondary", color = MaterialTheme.colorScheme.secondary, onColor = MaterialTheme.colorScheme.onSecondary)
                    ColorBox(name = "Tertiary", color = MaterialTheme.colorScheme.tertiary, onColor = MaterialTheme.colorScheme.onTertiary)
                }
            }
        }
    }
}

@Composable
fun ColorBox(name: String, color: Color, onColor: Color) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(color)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name, color = onColor)
    }
}