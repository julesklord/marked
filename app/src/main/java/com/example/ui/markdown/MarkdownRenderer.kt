package com.example.ui.markdown

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.R
import com.example.ui.theme.MyApplicationTheme

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
    modifier: Modifier = Modifier,
    onToggleChecklist: (Int) -> Unit
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
                    text = stringResource(R.string.empty_document_msg),
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
                        1 -> 22.dp
                        2 -> 18.dp
                        else -> 14.dp
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topPadding, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (block.level == 2) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = parseInlineText(block.text, codeBgColor, accentColor),
                                style = TextStyle(
                                    fontFamily = preferences.selectedFont.fontFamily,
                                    fontSize = (preferences.fontSizeSp * scale).sp,
                                    fontWeight = weight,
                                    color = headerColor,
                                    lineHeight = (preferences.fontSizeSp * scale * 1.35f).sp,
                                    letterSpacing = if (block.level == 1) 0.25.sp else 0.sp
                                )
                            )
                            if (block.level == 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(3.dp)
                                        .padding(top = 8.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    accentColor,
                                                    accentColor.copy(alpha = 0.3f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }
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
                                Box(
                                    modifier = Modifier
                                        .padding(top = (preferences.fontSizeSp * 0.45f).dp, end = 12.dp, start = 6.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleChecklist(item.lineIndex) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { onToggleChecklist(item.lineIndex) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor,
                                        checkmarkColor = bgColor
                                    ),
                                    modifier = Modifier.size(36.dp)
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        quoteBarColor.copy(alpha = 0.25f),
                                        quoteBarColor.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                            )
                    ) {
                        Text(
                            text = "“",
                            style = TextStyle(
                                fontSize = 90.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor.copy(alpha = 0.08f),
                                lineHeight = 0.sp
                            ),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 6.dp, y = (-20).dp)
                        )

                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
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
                                    .padding(top = 12.dp, bottom = 12.dp, end = 16.dp)
                            )
                        }
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E222A))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E222A))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = block.language.ifEmpty { "CODE" }.uppercase(),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFABB2BF).copy(alpha = 0.6f)
                                    )
                                )
                            }
                            val codeCopiedMessage = stringResource(R.string.code_copied)
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(block.code))
                                    Toast.makeText(context, codeCopiedMessage, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.copy_code),
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        SelectionContainer {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = highlightCode(block.code),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (preferences.fontSizeSp * 0.9f).sp,
                                        lineHeight = (preferences.fontSizeSp * 1.25f).sp
                                    )
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.Divider -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            quoteBarColor.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .rotate(45f)
                                .background(accentColor.copy(alpha = 0.7f))
                        )
                    }
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

fun highlightCode(code: String): AnnotatedString {
    val keywords = setOf(
        "fun", "function", "val", "var", "class", "import", "return", "package", 
        "override", "let", "const", "if", "else", "when", "for", "while", 
        "try", "catch", "finally", "throw", "null", "true", "false", 
        "this", "super", "interface", "object", "private", "protected", 
        "public", "internal", "enum", "sealed", "data", "suspend", "is", "as"
    )
    val types = setOf(
        "String", "Int", "Boolean", "Float", "Double", "Long", "Char", "Byte", 
        "Short", "Any", "Unit", "Nothing", "List", "Map", "Set", "StateFlow", 
        "Flow", "MutableStateFlow", "State", "ViewModel", "Context", "Modifier"
    )

    return buildAnnotatedString {
        val lines = code.split("\n")
        lines.forEachIndexed { lineIdx, line ->
            var i = 0
            val len = line.length
            while (i < len) {
                // Comments
                if (line.startsWith("//", i) || line.startsWith("#", i)) {
                    pushStyle(SpanStyle(color = Color(0xFF7F848E), fontStyle = FontStyle.Italic))
                    append(line.substring(i))
                    pop()
                    break
                }
                // Strings
                else if (line[i] == '"' || line[i] == '\'') {
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
                    pushStyle(SpanStyle(color = Color(0xFF98C379)))
                    append(line.substring(i, end))
                    pop()
                    i = end
                }
                // Annotations starting with @
                else if (line[i] == '@') {
                    var end = i + 1
                    while (end < len && (line[end].isLetterOrDigit() || line[end] == '_')) {
                        end++
                    }
                    pushStyle(SpanStyle(color = Color(0xFFE5C07B)))
                    append(line.substring(i, end))
                    pop()
                    i = end
                }
                // Numbers
                else if (line[i].isDigit()) {
                    var end = i
                    while (end < len && (line[end].isLetterOrDigit() || line[end] == '.')) {
                        end++
                    }
                    pushStyle(SpanStyle(color = Color(0xFFD19A66)))
                    append(line.substring(i, end))
                    pop()
                    i = end
                }
                // Words (Keywords, Types, identifiers)
                else if (line[i].isLetter()) {
                    var end = i
                    while (end < len && (line[end].isLetterOrDigit() || line[end] == '_')) {
                        end++
                    }
                    val word = line.substring(i, end)
                    when {
                        word in keywords -> {
                            pushStyle(SpanStyle(color = Color(0xFFC678DD), fontWeight = FontWeight.Bold))
                            append(word)
                            pop()
                        }
                        word in types -> {
                            pushStyle(SpanStyle(color = Color(0xFF61AFEF)))
                            append(word)
                            pop()
                        }
                        else -> {
                            pushStyle(SpanStyle(color = Color(0xFFABB2BF)))
                            append(word)
                            pop()
                        }
                    }
                    i = end
                }
                // Other characters
                else {
                    pushStyle(SpanStyle(color = Color(0xFFABB2BF).copy(alpha = 0.8f)))
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

private const val SAMPLE_MARKDOWN = """
# Heading 1
## Heading 2
This is a **bold** and *italic* paragraph with `inline code` and a [link](https://example.com).

- Item 1
- Item 2

1. First
2. Second

- [ ] Unchecked task
- [x] Checked task

> This is a blockquote.
> It can span multiple lines.

```kotlin
fun main() {
    println("Hello, World!")
}
```

---
"""

@Preview(showBackground = true, name = "Light Theme")
@Composable
fun PreviewMarkdownRendererLight() {
    MyApplicationTheme(darkTheme = false) {
        Surface {
            MarkdownRenderer(
                content = SAMPLE_MARKDOWN,
                preferences = ReaderPreferences(selectedTheme = ReaderTheme.PAPELES),
                onToggleChecklist = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
fun PreviewMarkdownRendererDark() {
    MyApplicationTheme(darkTheme = true) {
        Surface {
            MarkdownRenderer(
                content = SAMPLE_MARKDOWN,
                preferences = ReaderPreferences(selectedTheme = ReaderTheme.IMMERSIVE_UI),
                onToggleChecklist = {}
            )
        }
    }
}
