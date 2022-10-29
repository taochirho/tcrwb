package com.taochirho.wordbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable


private val DarkColorPalette = darkColors(
    primary = BlueGray500,
    primaryVariant = BlueGray700,
    secondary = LightGreen300,
    secondaryVariant = LightGreen600,
    background = LightGreen900,
//    surface = BlueGray50,
    surface = LightGreen600,
    onPrimary = BlueGray50,
    onSecondary = LightGreenA200,
    onBackground = Black,
    onSurface = LightGreenA400,
    error = Red700,
    onError = Red50,

)

private val LightColorPalette = lightColors(
    primary = BlueGray50,
    primaryVariant = BlueGray200,
    secondary = LightGreen900,
    secondaryVariant = LightGreen600,
    background = BlueGray900,
    surface = BlueGray900,
    onPrimary = Black,
    onSecondary = White,
    onBackground = BlueGray50,
    onSurface = BlueGray50,
    error = Red50,
    onError = Red700
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