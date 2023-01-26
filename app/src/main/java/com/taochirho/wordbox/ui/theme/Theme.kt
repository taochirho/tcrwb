package com.taochirho.wordbox.ux.views.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import com.taochirho.wordbox.ui.main.theme.*

private val LightColorPalette = lightColors(
    primary = BlueGray50,
    primaryVariant = BlueGray100,
    secondary = Amber300,
    secondaryVariant = Amber600,
    background = OffWhite,
    surface = LightGreen50,

    onPrimary = LightGreen1000,
    onSecondary = LightGreen1100,
    onBackground = BlueGray800,
    onSurface = LightGreen800,
    error = Red50,
    onError = Red600

)

private val DarkColorPalette = darkColors(
    primary = LightGreen700,
    primaryVariant = LightGreen1100,
    secondary = Amber600,
    secondaryVariant = BlueGray800,
    background = BlueGray900,
    surface = LightGreen1000,

    onPrimary = LightGreenA400,
    onSecondary = LightGreenA200,
    onBackground = BlueGray100,
    onSurface = BlueGray50,
    error = Red600,
    onError = Red50
)
@Composable
fun TCRCreatePuzzleTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}