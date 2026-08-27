package com.histopgambling.looplock.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Midnight = Color(0xFF07172D)
val DeepOcean = Color(0xFF0D2942)
val TranquilTeal = Color(0xFF2EC4B6)
val DeepTeal = Color(0xFF087F7A)
val SkyBlue = Color(0xFF76C7FF)
val SoftLavender = Color(0xFFB7A4FF)
val WarmCoral = Color(0xFFFF8A72)
val Sunrise = Color(0xFFFFD79A)
val SoftCream = Color(0xFFF8F6F1)
val Ink = Color(0xFF102038)
val Mist = Color(0xFFE9F3F1)

private val DarkLoopLockColors = darkColorScheme(
    primary = TranquilTeal,
    onPrimary = Midnight,
    secondary = SoftLavender,
    onSecondary = Midnight,
    background = Midnight,
    onBackground = Color(0xFFF7FBFF),
    surface = DeepOcean,
    onSurface = Color(0xFFF7FBFF),
    surfaceVariant = Color(0xFF173A57),
    onSurfaceVariant = Color(0xFFC9D9E8),
    error = WarmCoral,
    onError = Midnight,
)

private val LightLoopLockColors = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    secondary = Color(0xFF6750A4),
    onSecondary = Color.White,
    background = SoftCream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Color(0xFF41566B),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val LoopLockTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.1).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 37.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

private val LoopLockShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun LoopLockTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkLoopLockColors else LightLoopLockColors,
        typography = LoopLockTypography,
        shapes = LoopLockShapes,
        content = content,
    )
}
