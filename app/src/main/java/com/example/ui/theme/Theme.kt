package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun getCustomColorScheme(theme: AppColorTheme, isDark: Boolean) = when (theme) {
    AppColorTheme.NORDIC_SAGE -> if (isDark) {
        darkColorScheme(
            primary = SagePrimaryLight,
            onPrimary = Color(0xFF041E11),
            primaryContainer = SagePrimary,
            onPrimaryContainer = SageContainer,
            secondary = SageAccent,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF4A3222),
            onSecondaryContainer = Color(0xFFFFDCC7),
            background = SageBackgroundDark,
            surface = SageSurfaceDark,
            surfaceVariant = SageSurfaceVariantDark,
            onBackground = Color(0xFFE4EDE7),
            onSurface = Color(0xFFE4EDE7),
            outline = SageBorderDark,
            error = CrisisRed
        )
    } else {
        lightColorScheme(
            primary = SagePrimary,
            onPrimary = Color.White,
            primaryContainer = SageContainer,
            onPrimaryContainer = SageOnContainer,
            secondary = SageAccent,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFE0D1),
            onSecondaryContainer = Color(0xFF5A2708),
            background = SageBackgroundLight,
            surface = SageSurfaceLight,
            surfaceVariant = SageSurfaceVariantLight,
            onBackground = Color(0xFF1E2822),
            onSurface = Color(0xFF1E2822),
            outline = SageBorderLight,
            error = CrisisRed
        )
    }

    AppColorTheme.CELESTIAL_TWILIGHT -> if (isDark) {
        darkColorScheme(
            primary = CelestialPrimaryLight,
            onPrimary = Color(0xFF17103D),
            primaryContainer = CelestialPrimary,
            onPrimaryContainer = CelestialContainer,
            secondary = CelestialAccent,
            onSecondary = Color.White,
            background = CelestialBackgroundDark,
            surface = CelestialSurfaceDark,
            surfaceVariant = CelestialSurfaceVariantDark,
            onBackground = Color(0xFFECE7F7),
            onSurface = Color(0xFFECE7F7),
            error = CrisisRed
        )
    } else {
        lightColorScheme(
            primary = CelestialPrimary,
            onPrimary = Color.White,
            primaryContainer = CelestialContainer,
            onPrimaryContainer = CelestialOnContainer,
            secondary = CelestialAccent,
            onSecondary = Color.White,
            background = CelestialBackgroundLight,
            surface = CelestialSurfaceLight,
            surfaceVariant = CelestialSurfaceVariantLight,
            onBackground = Color(0xFF1A162B),
            onSurface = Color(0xFF1A162B),
            error = CrisisRed
        )
    }

    AppColorTheme.OCEAN_MIST -> if (isDark) {
        darkColorScheme(
            primary = OceanPrimaryLight,
            onPrimary = Color(0xFF031A1D),
            primaryContainer = OceanPrimary,
            onPrimaryContainer = OceanContainer,
            secondary = OceanAccent,
            onSecondary = Color.White,
            background = OceanBackgroundDark,
            surface = OceanSurfaceDark,
            surfaceVariant = OceanSurfaceVariantDark,
            onBackground = Color(0xFFE2F0F2),
            onSurface = Color(0xFFE2F0F2),
            error = CrisisRed
        )
    } else {
        lightColorScheme(
            primary = OceanPrimary,
            onPrimary = Color.White,
            primaryContainer = OceanContainer,
            onPrimaryContainer = OceanOnContainer,
            secondary = OceanAccent,
            onSecondary = Color.White,
            background = OceanBackgroundLight,
            surface = OceanSurfaceLight,
            surfaceVariant = OceanSurfaceVariantLight,
            onBackground = Color(0xFF102528),
            onSurface = Color(0xFF102528),
            error = CrisisRed
        )
    }

    AppColorTheme.WARM_TERRACOTTA -> if (isDark) {
        darkColorScheme(
            primary = TerraPrimaryLight,
            onPrimary = Color(0xFF280B03),
            primaryContainer = TerraPrimary,
            onPrimaryContainer = TerraContainer,
            secondary = TerraAccent,
            onSecondary = Color.White,
            background = TerraBackgroundDark,
            surface = TerraSurfaceDark,
            surfaceVariant = TerraSurfaceVariantDark,
            onBackground = Color(0xFFF3ECE6),
            onSurface = Color(0xFFF3ECE6),
            error = CrisisRed
        )
    } else {
        lightColorScheme(
            primary = TerraPrimary,
            onPrimary = Color.White,
            primaryContainer = TerraContainer,
            onPrimaryContainer = TerraOnContainer,
            secondary = TerraAccent,
            onSecondary = Color.White,
            background = TerraBackgroundLight,
            surface = TerraSurfaceLight,
            surfaceVariant = TerraSurfaceVariantLight,
            onBackground = Color(0xFF2E1C15),
            onSurface = Color(0xFF2E1C15),
            error = CrisisRed
        )
    }
}

@Composable
fun MyApplicationTheme(
    theme: AppColorTheme = AppColorTheme.NORDIC_SAGE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = getCustomColorScheme(theme, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
