package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// 🌿 Palette 1: Nordic Sage & Botanical (Default)
val SagePrimary = Color(0xFF26533A)
val SagePrimaryLight = Color(0xFF437A5B)
val SageContainer = Color(0xFFDCEFE3)
val SageOnContainer = Color(0xFF103320)
val SageAccent = Color(0xFFD97736) // Warm Terracotta Sunset
val SageBackgroundLight = Color(0xFFF9F7F2) // Organic Warm Porcelain
val SageSurfaceLight = Color(0xFFFFFFFF)
val SageSurfaceVariantLight = Color(0xFFF0ECE1) // Creamy Linen Card
val SageBorderLight = Color(0xFFE2DDD2)

val SageBackgroundDark = Color(0xFF101714) // Deep Evergreen Night
val SageSurfaceDark = Color(0xFF18221E)
val SageSurfaceVariantDark = Color(0xFF202D28)
val SageBorderDark = Color(0xFF2C3C36)

// 🌌 Palette 2: Celestial Twilight (Violet & Lavender)
val CelestialPrimary = Color(0xFF6356A5)
val CelestialPrimaryLight = Color(0xFF8B7EC8)
val CelestialContainer = Color(0xFFECE7FA)
val CelestialOnContainer = Color(0xFF241C52)
val CelestialAccent = Color(0xFFE58F65)
val CelestialBackgroundLight = Color(0xFFF8F7FC)
val CelestialSurfaceLight = Color(0xFFFFFFFF)
val CelestialSurfaceVariantLight = Color(0xFFEFEBF7)

val CelestialBackgroundDark = Color(0xFF12101C)
val CelestialSurfaceDark = Color(0xFF1B1829)
val CelestialSurfaceVariantDark = Color(0xFF26223B)

// 🌊 Palette 3: Restorative Ocean & Seafoam
val OceanPrimary = Color(0xFF1A6B75)
val OceanPrimaryLight = Color(0xFF3393A0)
val OceanContainer = Color(0xFFD6F2F5)
val OceanOnContainer = Color(0xFF0A373E)
val OceanAccent = Color(0xFFF59E0B)
val OceanBackgroundLight = Color(0xFFF4F9FA)
val OceanSurfaceLight = Color(0xFFFFFFFF)
val OceanSurfaceVariantLight = Color(0xFFE5F0F2)

val OceanBackgroundDark = Color(0xFF0C1719)
val OceanSurfaceDark = Color(0xFF132225)
val OceanSurfaceVariantDark = Color(0xFF1C3135)

// 🌅 Palette 4: Warm Desert Terracotta & Sand
val TerraPrimary = Color(0xFF9E4B2F)
val TerraPrimaryLight = Color(0xFFC76D4E)
val TerraContainer = Color(0xFFFBE4DC)
val TerraOnContainer = Color(0xFF481C0E)
val TerraAccent = Color(0xFF2D6A4F)
val TerraBackgroundLight = Color(0xFFFAF6F2)
val TerraSurfaceLight = Color(0xFFFFFFFF)
val TerraSurfaceVariantLight = Color(0xFFF2EAE2)

val TerraBackgroundDark = Color(0xFF1A120E)
val TerraSurfaceDark = Color(0xFF261B16)
val TerraSurfaceVariantDark = Color(0xFF35261F)

// Universal Safety & Emotion Badges
val CrisisRed = Color(0xFFDC2626)
val CrisisRedBackground = Color(0xFFFEF2F2)
val CrisisRedBorder = Color(0xFFFCA5A5)

// Backward-compatible branding aliases
val MindCareTeal = SagePrimary
val MindCareTealLight = SagePrimaryLight
val MindCareSage = SagePrimaryLight
val MindCarePurple = CelestialPrimary
val MindCarePink = Color(0xFFEC4899)
val MindCareAmber = Color(0xFFF59E0B)
val MindCareBlue = Color(0xFF3B82F6)

val EmotionJoy = Color(0xFF10B981)
val EmotionCalm = Color(0xFF26533A)
val EmotionAnxiety = Color(0xFFF59E0B)
val EmotionStress = Color(0xFFEF4444)
val EmotionSadness = Color(0xFF3B82F6)
val EmotionAnger = Color(0xFFB91C1C)
val EmotionNeutral = Color(0xFF6B7280)

enum class AppColorTheme(val displayName: String, val emoji: String, val previewColor: Color) {
    NORDIC_SAGE("Nordic Sage", "🌿", SagePrimary),
    CELESTIAL_TWILIGHT("Celestial Twilight", "🌌", CelestialPrimary),
    OCEAN_MIST("Ocean Mist", "🌊", OceanPrimary),
    WARM_TERRACOTTA("Desert Terracotta", "🌅", TerraPrimary)
}
