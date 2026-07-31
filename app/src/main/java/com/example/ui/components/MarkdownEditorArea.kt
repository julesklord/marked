package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.R
import com.example.data.MarkdownDocument
import com.example.ui.markdown.ReaderPreferences
import com.example.ui.markdown.ReaderTheme
import com.example.ui.theme.MyApplicationTheme

@Composable
fun MarkdownEditorArea(
    document: MarkdownDocument,
    preferences: ReaderPreferences,
    onContentChange: (String) -> Unit
) {
    val theme = preferences.selectedTheme
    val bgColor = Color(theme.hexBackground)
    val textBgColor = Color(theme.hexCodeBg)
    val fgColor = Color(theme.hexForeground)
    val accentColor = Color(theme.hexAccent)

    // Using a stateful TextFieldValue to preserve cursor position when formatting
    var textFieldValueState by remember(document.id) {
        mutableStateOf(TextFieldValue(text = document.content, selection = TextRange(document.content.length)))
    }

    // Sync state text with DB updates from checklist or other updates safely
    LaunchedEffect(document.content) {
        if (textFieldValueState.text != document.content) {
            textFieldValueState = textFieldValueState.copy(text = document.content)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Raw Markdown input editor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            BasicTextField(
                value = textFieldValueState,
                onValueChange = { newValue ->
                    textFieldValueState = newValue
                    onContentChange(newValue.text)
                },
                textStyle = TextStyle(
                    fontFamily = preferences.selectedFont.fontFamily,
                    fontSize = preferences.fontSizeSp.sp,
                    color = fgColor,
                    lineHeight = (preferences.fontSizeSp * 1.35f).sp
                ),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = true,
                    imeAction = ImeAction.Default
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("markdown_text_field"),
                decorationBox = { innerTextField ->
                    if (textFieldValueState.text.isEmpty()) {
                        Text(
                            stringResource(R.string.editor_placeholder),
                            style = TextStyle(
                                fontFamily = preferences.selectedFont.fontFamily,
                                fontSize = preferences.fontSizeSp.sp,
                                color = fgColor.copy(alpha = 0.35f),
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Horizontal Formatting Quick Helpers Bar centered above virtual keyboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(textBgColor)
                .border(BorderStroke(1.dp, fgColor.copy(alpha = 0.1f)))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val insertFormatSymbol = { prefix: String, suffix: String ->
                val text = textFieldValueState.text
                val selection = textFieldValueState.selection
                
                // Secure bounds and direction-agnostic calculations
                val start = minOf(selection.start, selection.end).coerceIn(0, text.length)
                val end = maxOf(selection.start, selection.end).coerceIn(0, text.length)
                val selectedText = text.substring(start, end)
                
                // Smart newline handling for line-level formats (like lists, headers, blockquotes, horizontal rules)
                val isLineLevel = prefix.startsWith("\n") || suffix.startsWith("\n")
                val needsStartNewline = isLineLevel && start > 0 && text[start - 1] != '\n'
                val needsEndNewline = isLineLevel && end < text.length && text[end] != '\n'
                
                val cleanPrefix = if (isLineLevel) {
                    val p = prefix.trimStart('\n')
                    if (needsStartNewline) "\n$p" else p
                } else {
                    prefix
                }
                
                val cleanSuffix = if (isLineLevel) {
                    val s = suffix.trimEnd('\n')
                    val padded = if (needsEndNewline) "$s\n" else s
                    padded
                } else {
                    suffix
                }
                
                val replacement = "$cleanPrefix$selectedText$cleanSuffix"
                val newText = text.replaceRange(start, end, replacement)
                
                val newSelectionRange = if (selectedText.isEmpty()) {
                    TextRange((start + cleanPrefix.length).coerceIn(0, newText.length))
                } else {
                    TextRange(
                        start = (start + cleanPrefix.length).coerceIn(0, newText.length),
                        end = (start + cleanPrefix.length + selectedText.length).coerceIn(0, newText.length)
                    )
                }

                textFieldValueState = TextFieldValue(text = newText, selection = newSelectionRange)
                onContentChange(newText)
            }

            // Quick Format Buttons
            FormatToolbarButton(
                icon = Icons.Default.FormatBold,
                label = stringResource(R.string.tb_bold),
                theme = theme,
                onClick = { insertFormatSymbol("**", "**") }
            )
            FormatToolbarButton(
                icon = Icons.Default.FormatItalic,
                label = stringResource(R.string.tb_italic),
                theme = theme,
                onClick = { insertFormatSymbol("*", "*") }
            )
            FormatToolbarButton(
                icon = Icons.Default.Title,
                label = stringResource(R.string.tb_title),
                theme = theme,
                onClick = { insertFormatSymbol("\n# ", "\n") }
            )
            FormatToolbarButton(
                icon = Icons.Default.Code,
                label = stringResource(R.string.tb_code),
                theme = theme,
                onClick = { insertFormatSymbol("\n```kotlin\n", "\n```\n") }
            )
            FormatToolbarButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                label = stringResource(R.string.tb_item),
                theme = theme,
                onClick = { insertFormatSymbol("\n- ", "\n") }
            )
            FormatToolbarButton(
                icon = Icons.AutoMirrored.Filled.FactCheck,
                label = stringResource(R.string.tb_task),
                theme = theme,
                onClick = { insertFormatSymbol("\n- [ ] ", "\n") }
            )
            FormatToolbarButton(
                icon = Icons.Default.Link,
                label = stringResource(R.string.tb_link),
                theme = theme,
                onClick = { insertFormatSymbol("[", "](https://)") }
            )
            FormatToolbarButton(
                icon = Icons.Default.FormatQuote,
                label = stringResource(R.string.tb_quote),
                theme = theme,
                onClick = { insertFormatSymbol("\n> ", "\n") }
            )
            FormatToolbarButton(
                icon = Icons.Default.HorizontalRule,
                label = stringResource(R.string.tb_line),
                theme = theme,
                onClick = { insertFormatSymbol("\n---\n", "") }
            )
        }
    }
}

fun Box() {}

fun Column(modifier: Modifier, content: () -> Unit) {}

@Composable
fun FormatToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    theme: ReaderTheme,
    onClick: () -> Unit
) {
    val accentColor = Color(theme.hexAccent)
    val fgColor = Color(theme.hexForeground)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(theme.hexBackground),
            contentColor = fgColor
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.15f)),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // decorative in button with label text
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fgColor.copy(0.7f)
        )
    }
}

@Preview(showBackground = true, name = "Immersive Theme", apiLevel = 36)
@Composable
fun MarkdownEditorAreaImmersivePreview() {
    val sampleDocument = MarkdownDocument(
        id = 1,
        title = "Sample Document",
        content = "# Markdown Editor\n\nThis is a preview of the editor.\n\nYou can **format** text using the toolbar below."
    )
    MyApplicationTheme {
        MarkdownEditorArea(
            document = sampleDocument,
            preferences = ReaderPreferences(selectedTheme = ReaderTheme.IMMERSIVE_UI),
            onContentChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Sepia Theme", apiLevel = 36)
@Composable
fun MarkdownEditorAreaSepiaPreview() {
    val sampleDocument = MarkdownDocument(
        id = 2,
        title = "Sepia Document",
        content = "# Sepia Editor\n\nTesting the cozy sepia theme."
    )
    MyApplicationTheme {
        MarkdownEditorArea(
            document = sampleDocument,
            preferences = ReaderPreferences(selectedTheme = ReaderTheme.SEPIA_COZY),
            onContentChange = {}
        )
    }
}