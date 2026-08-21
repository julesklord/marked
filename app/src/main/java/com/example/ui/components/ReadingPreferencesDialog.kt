package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.markdown.ReaderFontFamily
import com.example.ui.markdown.ReaderPreferences
import com.example.ui.markdown.ReaderTheme

@Composable
fun ReadingPreferencesDialog(
    preferences: ReaderPreferences,
    onDismissRequest: () -> Unit,
    onUpdatePreferences: ((ReaderPreferences) -> ReaderPreferences) -> Unit
) {
    val theme = preferences.selectedTheme
    val bgColor = Color(theme.hexBackground)
    val fgColor = Color(theme.hexForeground)
    val accentColor = Color(theme.hexAccent)
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.accept), color = accentColor, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = stringResource(R.string.pref_dialog_title),
                style = TextStyle(
                    fontFamily = preferences.selectedFont.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(theme.hexHeader)
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Typography Selection Row
                Text(
                    text = stringResource(R.string.pref_font_title),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = fgColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (font in ReaderFontFamily.entries) {
                        val selected = preferences.selectedFont == font
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) accentColor else Color(theme.hexCodeBg))
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onUpdatePreferences { it.copy(selectedFont = font) } }
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (selected) accentColor else fgColor.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(font.displayNameResId),
                                style = TextStyle(
                                    fontFamily = font.fontFamily,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = if (selected) Color(theme.hexBackground) else fgColor
                                )
                            )
                        }
                    }
                }

                // Font size control with live slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pref_font_size, preferences.fontSizeSp.toInt()),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = fgColor.copy(alpha = 0.7f)
                    )
                }
                Slider(
                    value = preferences.fontSizeSp,
                    onValueChange = { size ->
                        onUpdatePreferences { it.copy(fontSizeSp = size) }
                    },
                    valueRange = 14f..28f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = fgColor.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Line Spacing multipliers
                Text(
                    text = stringResource(R.string.pref_spacing_title),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = fgColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        stringResource(R.string.spacing_compact) to 1.2f,
                        stringResource(R.string.spacing_standard) to 1.5f,
                        stringResource(R.string.spacing_spacious) to 1.8f
                    )
                    for (option in options) {
                        val label = option.first
                        val multiplier = option.second
                        val selected = preferences.lineSpacingMultiplier == multiplier
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) accentColor else Color(theme.hexCodeBg))
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onUpdatePreferences { it.copy(lineSpacingMultiplier = multiplier) } }
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (selected) accentColor else fgColor.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color(theme.hexBackground) else fgColor
                                )
                            )
                        }
                    }
                }

                // Theme Mode colors pallet
                Text(
                    text = stringResource(R.string.pref_theme_title),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = fgColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (rt in ReaderTheme.entries) {
                        val selected = preferences.selectedTheme == rt
                        Column(
                            modifier = Modifier
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onUpdatePreferences { it.copy(selectedTheme = rt) } }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(rt.hexBackground))
                                    .border(
                                        BorderStroke(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) accentColor else fgColor.copy(alpha = 0.2f)
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ab",
                                    color = Color(rt.hexForeground),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = preferences.selectedFont.fontFamily
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(rt.displayNameResId),
                                fontSize = 10.sp,
                                maxLines = 1,
                                color = if (selected) accentColor else fgColor.copy(alpha = 0.6f),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        containerColor = bgColor,
        modifier = Modifier.testTag("reading_preferences_dialog")
    )
}

