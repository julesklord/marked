package com.example.ui.markdown

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

enum class ReaderFontFamily(val displayNameResId: Int, val fontFamily: FontFamily) {
    SANS_SERIF(R.string.font_sans, FontFamily.SansSerif),
    SERIF(R.string.font_serif, FontFamily.Serif),
    MONOSPACE(R.string.font_monospace, FontFamily.Monospace)
}

enum class ReaderTheme(
    val displayNameResId: Int,
    val isDark: Boolean,
    val hexBackground: Long,
    val hexForeground: Long,
    val hexHeader: Long,
    val hexAccent: Long,
    val hexQuoteBar: Long,
    val hexCodeBg: Long
) {
    IMMERSIVE_UI(R.string.theme_immersive, true, 0xFF0F1113, 0xFFC2C7CF, 0xFFFFFFFF, 0xFFD1E4FF, 0xFF2C2E33, 0xFF16181B),
    PAPELES(R.string.theme_paper, false, 0xFFFAF9F6, 0xFF1C1A17, 0xFF000000, 0xFF8F5A3C, 0xFFD4C8BC, 0xFFF1EDE6),
    SEPIA_COZY(R.string.theme_sepia, false, 0xFFFDF6E3, 0xFF586E75, 0xFF073642, 0xFFB58900, 0xFF93A1A1, 0xFFEEE8D5),
    CHARCOAL_NIGHT(R.string.theme_charcoal, true, 0xFF1E1E1E, 0xFFD4D4D4, 0xFFFFFFFF, 0xFFF39C12, 0xFF4A4A4A, 0xFF2D2D2D),
    PITCH_BLACK(R.string.theme_black, true, 0xFF000000, 0xFFE0E0E0, 0xFFFFFFFF, 0xFF29B6F6, 0xFF37474F, 0xFF121212)
}

data class ReaderPreferences(
    val selectedFont: ReaderFontFamily = ReaderFontFamily.SANS_SERIF,
    val fontSizeSp: Float = 18f,
    val lineSpacingMultiplier: Float = 1.5f,
    val selectedTheme: ReaderTheme = ReaderTheme.IMMERSIVE_UI
)
