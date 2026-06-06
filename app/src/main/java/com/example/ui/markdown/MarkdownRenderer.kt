package com.example.ui.markdown

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    content: String,
    preferences: ReaderPreferences,
    onToggleChecklist: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks = MarkdownParser.parse(content)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val theme = preferences.selectedTheme
    val bgColor = Color(theme.hexBackground)
    val fgColor = Color(theme.hexForeground)
    val headerColor = Color(theme.hexHeader)
    val accentColor = Color(theme.hexAccent)
    val quoteBarColor = Color(theme.hexQuoteBar)
    val codeBgColor = Color(theme.hexCodeBg)

    val backgroundModifier = if (theme == ReaderTheme.IMMERSIVE_UI) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F1113),
                    Color(0xFF16181B)
                )
            )
        )
    } else {
        Modifier.background(bgColor)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .padding(16.dp)
    ) {
        if (blocks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Documento vacío. Haz click en Editar para escribir.",
                    style = TextStyle(
                        fontFamily = preferences.selectedFont.fontFamily,
                        fontSize = preferences.fontSizeSp.sp,
                        color = fgColor.copy(alpha = 0.5f),
                        fontStyle = FontStyle.Italic
                    )
                )
            }
        }

        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val scale = when (block.level) {
                        1 -> 1.5f
                        2 -> 1.3f
                        3 -> 1.15f
                        else -> 1.1f
                    }
                    val weight = if (block.level <= 3) FontWeight.Bold else FontWeight.Medium
                    val topPadding = when (block.level) {
                        1 -> 20.dp
                        2 -> 16.dp
                        else -> 12.dp
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topPadding, bottom = 8.dp)
                    ) {
                        Text(
                            text = parseInlineText(block.text, codeBgColor, accentColor),
                            style = TextStyle(
                                fontFamily = preferences.selectedFont.fontFamily,
                                fontSize = (preferences.fontSizeSp * scale).sp,
                                fontWeight = weight,
                                color = headerColor,
                                lineHeight = (preferences.fontSizeSp * scale * 1.3f).sp
                            )
                        )
                        if (block.level == 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                                thickness = 2.dp,
                                color = accentColor.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineText(block.text, codeBgColor, accentColor),
                        style = TextStyle(
                            fontFamily = preferences.selectedFont.fontFamily,
                            fontSize = preferences.fontSizeSp.sp,
                            color = fgColor,
                            lineHeight = (preferences.fontSizeSp * preferences.lineSpacingMultiplier).sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                is MarkdownBlock.BulletList -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        block.items.forEach { itemText ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    style = TextStyle(
                                        fontSize = (preferences.fontSizeSp * 1.1f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    ),
                                    modifier = Modifier.padding(end = 10.dp, start = 4.dp)
                                )
                                Text(
                                    text = parseInlineText(itemText, codeBgColor, accentColor),
                                    style = TextStyle(
                                        fontFamily = preferences.selectedFont.fontFamily,
                                        fontSize = preferences.fontSizeSp.sp,
                                        color = fgColor,
                                        lineHeight = (preferences.fontSizeSp * preferences.lineSpacingMultiplier).sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.OrderedList -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        block.items.forEachIndexed { index, itemText ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = preferences.fontSizeSp.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    ),
                                    modifier = Modifier.padding(end = 10.dp, start = 4.dp)
                                )
                                Text(
                                    text = parseInlineText(itemText, codeBgColor, accentColor),
                                    style = TextStyle(
                                        fontFamily = preferences.selectedFont.fontFamily,
                                        fontSize = preferences.fontSizeSp.sp,
                                        color = fgColor,
                                        lineHeight = (preferences.fontSizeSp * preferences.lineSpacingMultiplier).sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.Checklist -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        block.items.forEach { item ->
                            val textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                            val textAlpha = if (item.isChecked) 0.5f else 1.0f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleChecklist(item.lineIndex) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { onToggleChecklist(item.lineIndex) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor,
                                        checkmarkColor = bgColor
                                    ),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = parseInlineText(item.text, codeBgColor, accentColor),
                                    style = TextStyle(
                                        fontFamily = preferences.selectedFont.fontFamily,
                                        fontSize = preferences.fontSizeSp.sp,
                                        color = fgColor.copy(alpha = textAlpha),
                                        textDecoration = textDecoration,
                                        lineHeight = (preferences.fontSizeSp * preferences.lineSpacingMultiplier).sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .height(IntrinsicSize.Min)
                            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .background(quoteBarColor.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(4.dp)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = parseInlineText(block.text, codeBgColor, accentColor),
                            style = TextStyle(
                                fontFamily = preferences.selectedFont.fontFamily,
                                fontSize = preferences.fontSizeSp.sp,
                                color = fgColor.copy(alpha = 0.95f),
                                fontStyle = FontStyle.Italic,
                                lineHeight = (preferences.fontSizeSp * preferences.lineSpacingMultiplier).sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp, end = 12.dp)
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBgColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(codeBgColor.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = block.language.ifEmpty { "CODE" }.uppercase(),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fgColor.copy(alpha = 0.5f)
                                )
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(block.code))
                                    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar Código",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        SelectionContainer {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = highlightCode(block.code, theme, fgColor),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (preferences.fontSizeSp * 0.9f).sp,
                                        lineHeight = (preferences.fontSizeSp * 1.2f).sp
                                    )
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        thickness = 1.dp,
                        color = quoteBarColor.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

fun parseInlineText(
    text: String,
    codeBg: Color,
    accentColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length
        while (i < len) {
            when {
                // Bold **
                i + 1 < len && text[i] == '*' && text[i+1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }
                // Italic *
                text[i] == '*' -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && end > i + 1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append("*")
                        i += 1
                    }
                }
                // Inline Code `
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1 && end > i + 1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBg,
                                color = accentColor
                            )
                        )
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append("`")
                        i += 1
                    }
                }
                // Link [label](url)
                text[i] == '[' -> {
                    val closeBracket = text.indexOf(']', i + 1)
                    if (closeBracket != -1) {
                        if (closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                            val closeParen = text.indexOf(')', closeBracket + 2)
                            if (closeParen != -1) {
                                val label = text.substring(i + 1, closeBracket)
                                pushStyle(
                                    SpanStyle(
                                        color = accentColor,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                append(label)
                                pop()
                                i = closeParen + 1
                                continue
                            }
                        }
                    }
                    append("[")
                    i += 1
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

fun highlightCode(code: String, theme: ReaderTheme, fallbackColor: Color): AnnotatedString {
    if (!theme.isDark) {
        return AnnotatedString(code)
    }
    return buildAnnotatedString {
        val keywords = setOf("fun", "function", "val", "var", "class", "import", "return", "package", "override", "let", "const")
        val lines = code.split("\n")
        lines.forEachIndexed { lineIdx, line ->
            var i = 0
            val len = line.length
            while (i < len) {
                if (line.startsWith("//", i) || line.startsWith("#", i)) {
                    pushStyle(SpanStyle(color = Color(0xFF9E9E9E)))
                    append(line.substring(i))
                    pop()
                    break
                } else if (line[i] == '"' || line[i] == '\'') {
                    val quote = line[i]
                    var end = i + 1
                    while (end < len && line[end] != quote) {
                        if (line[end] == '\\' && end + 1 < len) {
                            end += 2
                        } else {
                            end++
                        }
                    }
                    if (end < len) end++
                    pushStyle(SpanStyle(color = Color(0xFFD1B26F)))
                    append(line.substring(i, end))
                    pop()
                    i = end
                } else if (line[i].isLetter()) {
                    var end = i
                    while (end < len && (line[end].isLetterOrDigit() || line[end] == '_')) {
                        end++
                    }
                    val word = line.substring(i, end)
                    if (word in keywords) {
                        pushStyle(SpanStyle(color = Color(0xFF7DBA84), fontWeight = FontWeight.Bold))
                        append(word)
                        pop()
                    } else {
                        pushStyle(SpanStyle(color = fallbackColor))
                        append(word)
                        pop()
                    }
                    i = end
                } else {
                    pushStyle(SpanStyle(color = fallbackColor.copy(alpha = 0.8f)))
                    append(line[i].toString())
                    pop()
                    i++
                }
            }
            if (lineIdx < lines.lastIndex) {
                append("\n")
            }
        }
    }
}
