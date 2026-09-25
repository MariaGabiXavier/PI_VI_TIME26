package com.example.flowcampus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.example.flowcampus.R
import androidx.compose.ui.text.ExperimentalTextApi

@OptIn(ExperimentalTextApi::class)
val OutfitFontFamily = FontFamily(
    Font(
        resId = R.font.outfit_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400)
        )
    ),
    Font(
        resId = R.font.outfit_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500)
        )
    ),
    Font(
        resId = R.font.outfit_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600)
        )
    ),
    Font(
        resId = R.font.outfit_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700)
        )
    )
)

private val baseTypography = Typography()

val Typography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = OutfitFontFamily),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = OutfitFontFamily),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = OutfitFontFamily),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = OutfitFontFamily),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = OutfitFontFamily),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = OutfitFontFamily),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = OutfitFontFamily),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = OutfitFontFamily),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = OutfitFontFamily),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = OutfitFontFamily),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = OutfitFontFamily),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = OutfitFontFamily),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = OutfitFontFamily),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = OutfitFontFamily),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = OutfitFontFamily)
)
