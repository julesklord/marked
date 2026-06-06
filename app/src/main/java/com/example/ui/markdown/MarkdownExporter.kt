package com.example.ui.markdown

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.MarkdownDocument
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object MarkdownExporter {

    private fun colorToCssHex(colorLong: Long): String {
        val r = (colorLong shr 16) and 0xFF
        val g = (colorLong shr 8) and 0xFF
        val b = colorLong and 0xFF
        return String.format("#%02X%02X%02X", r, g, b)
    }

    fun generateHtmlWithTheme(document: MarkdownDocument, preferences: ReaderPreferences): String {
        val blocks = MarkdownParser.parse(document.content)
        val theme = preferences.selectedTheme
        
        val bgColor = colorToCssHex(theme.hexBackground)
        val fgColor = colorToCssHex(theme.hexForeground)
        val headerColor = colorToCssHex(theme.hexHeader)
        val accentColor = colorToCssHex(theme.hexAccent)
        val quoteBarColor = colorToCssHex(theme.hexQuoteBar)
        val codeBgColor = colorToCssHex(theme.hexCodeBg)

        val fontStack = when (preferences.selectedFont) {
            ReaderFontFamily.SERIF -> "'Georgia', 'Times New Roman', Times, serif"
            ReaderFontFamily.MONOSPACE -> "'Courier New', Courier, 'JetBrains Mono', monospace"
            ReaderFontFamily.SANS_SERIF -> "system-ui, -apple-system, sans-serif"
        }

        val backgroundStyle = if (theme == ReaderTheme.IMMERSIVE_UI) {
            "background: linear-gradient(180deg, #0F1113 0%, #16181B 100%) attachment fixed;"
        } else {
            "background-color: $bgColor;"
        }

        val html = StringBuilder()
        html.append("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${escapeHtml(document.title)}</title>
                <style>
                    body {
                        $backgroundStyle
                        color: $fgColor;
                        font-family: $fontStack;
                        font-size: ${preferences.fontSizeSp}px;
                        line-height: ${preferences.lineSpacingMultiplier};
                        padding: 24px;
                        margin: 0;
                        max-width: 800px;
                        margin-left: auto;
                        margin-right: auto;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        color: $headerColor;
                        font-weight: bold;
                        margin-top: 1.5em;
                        margin-bottom: 0.5em;
                    }
                    h1 { 
                        font-size: 1.8em; 
                        border-bottom: 2px solid ${accentColor}40;
                        padding-bottom: 8px;
                    }
                    h2 { font-size: 1.4em; }
                    h3 { font-size: 1.25em; }
                    
                    p {
                        margin-top: 0;
                        margin-bottom: 1em;
                    }
                    
                    blockquote {
                        border-left: 4px solid $accentColor;
                        background-color: ${quoteBarColor}30;
                        padding: 12px 16px;
                        margin: 1.5em 0;
                        border-radius: 0 8px 8px 0;
                        font-style: italic;
                    }
                    
                    pre {
                        background-color: $codeBgColor;
                        border: 1px solid ${quoteBarColor}40;
                        padding: 16px;
                        border-radius: 12px;
                        overflow-x: auto;
                        margin: 1.5em 0;
                    }
                    
                    code {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 0.9em;
                        background-color: ${codeBgColor}80;
                        padding: 2px 6px;
                        border-radius: 4px;
                    }
                    
                    pre code {
                        background-color: transparent;
                        padding: 0;
                        border-radius: 0;
                        display: block;
                        font-size: 0.85em;
                        color: $fgColor;
                    }
                    
                    ul, ol {
                        margin-top: 0;
                        margin-bottom: 1em;
                        padding-left: 24px;
                    }
                    
                    li {
                        margin-bottom: 0.5em;
                    }
                    
                    a {
                        color: $accentColor;
                        text-decoration: underline;
                    }
                    
                    hr {
                        border: 0;
                        height: 1px;
                        background: ${quoteBarColor}50;
                        margin: 2em 0;
                    }
                    
                    .checklist-item {
                        list-style-type: none;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        margin-bottom: 0.5em;
                    }
                    
                    .checkbox {
                        width: 16px;
                        height: 16px;
                        border: 1.5px solid $accentColor;
                        border-radius: 4px;
                        display: inline-block;
                        flex-shrink: 0;
                    }
                    
                    .checkbox.checked {
                        background-color: $accentColor;
                        position: relative;
                    }
                    
                    .checkbox.checked::after {
                        content: '';
                        position: absolute;
                        left: 5px;
                        top: 1px;
                        width: 4px;
                        height: 9px;
                        border: solid white;
                        border-width: 0 2px 2px 0;
                        transform: rotate(45deg);
                    }
                    
                    .checked-text {
                        text-decoration: line-through;
                        opacity: 0.6;
                    }
                    
                    /* Syntax Highlight for Dark Themes */
                    .keyword { color: #7DBA84; font-weight: bold; }
                    .string { color: #D1B26F; }
                    .comment { color: #9E9E9E; font-style: italic; }

                    @media print {
                        body {
                            background: white !important;
                            color: black !important;
                            padding: 0;
                            font-size: 12pt;
                        }
                        h1, h2, h3 {
                            color: black !important;
                            page-break-after: avoid;
                        }
                        pre, blockquote {
                            page-break-inside: avoid;
                        }
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        // Header info matching current view style
        html.append("""
            <div style="margin-bottom: 32px;">
                <h1 style="margin-top: 0; margin-bottom: 4px;">${escapeHtml(document.title)}</h1>
                <div style="font-size: 0.8em; color: ${fgColor}99; display: flex; gap: 12px; margin-bottom: 16px;">
                    <span>Actualizado: ${SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(document.updatedAt)}</span>
                </div>
            </div>
        """.trimIndent())

        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    html.append("<h${block.level}>${parseInlineToHtml(block.text, accentColor)}</h${block.level}>")
                }
                is MarkdownBlock.Paragraph -> {
                    html.append("<p>${parseInlineToHtml(block.text, accentColor)}</p>")
                }
                is MarkdownBlock.BulletList -> {
                    html.append("<ul>")
                    block.items.forEach { item ->
                        html.append("<li>${parseInlineToHtml(item, accentColor)}</li>")
                    }
                    html.append("</ul>")
                }
                is MarkdownBlock.OrderedList -> {
                    html.append("<ol>")
                    block.items.forEach { item ->
                        html.append("<li>${parseInlineToHtml(item, accentColor)}</li>")
                    }
                    html.append("</ol>")
                }
                is MarkdownBlock.Checklist -> {
                    html.append("<ul style=\"padding-left: 0;\">")
                    block.items.forEach { item ->
                        val checkedClass = if (item.isChecked) "checked" else ""
                        val textStyleClass = if (item.isChecked) "checked-text" else ""
                        html.append("""
                            <li class="checklist-item">
                                <span class="checkbox $checkedClass"></span>
                                <span class="$textStyleClass">${parseInlineToHtml(item.text, accentColor)}</span>
                            </li>
                        """.trimIndent())
                    }
                    html.append("</ul>")
                }
                is MarkdownBlock.CodeBlock -> {
                    html.append("<pre><code class=\"language-${escapeHtml(block.language)}\">")
                    html.append(highlightHtmlCode(block.code, theme.isDark))
                    html.append("</code></pre>")
                }
                is MarkdownBlock.Quote -> {
                    val styledQuote = block.text.split("\n").joinToString("<br>") { parseInlineToHtml(it, accentColor) }
                    html.append("<blockquote>$styledQuote</blockquote>")
                }
                is MarkdownBlock.Divider -> {
                    html.append("<hr />")
                }
            }
        }

        html.append("""
            </body>
            </html>
        """.trimIndent())

        return html.toString()
    }

    private fun parseInlineToHtml(text: String, accentColor: String): String {
        var i = 0
        val len = text.length
        val out = StringBuilder()
        while (i < len) {
            when {
                // Bold **
                i + 1 < len && text[i] == '*' && text[i+1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        out.append("<strong>").append(parseInlineToHtml(text.substring(i + 2, end), accentColor)).append("</strong>")
                        i = end + 2
                    } else {
                        out.append("**")
                        i += 2
                    }
                }
                // Italic *
                text[i] == '*' -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && end > i + 1) {
                        out.append("<em>").append(parseInlineToHtml(text.substring(i + 1, end), accentColor)).append("</em>")
                        i = end + 1
                    } else {
                        out.append("*")
                        i += 1
                    }
                }
                // Inline Code `
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1 && end > i + 1) {
                        out.append("<code>").append(escapeHtml(text.substring(i + 1, end))).append("</code>")
                        i = end + 1
                    } else {
                        out.append("`")
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
                                val url = text.substring(closeBracket + 2, closeParen)
                                out.append("<a href=\"").append(escapeHtml(sanitizeUrl(url))).append("\">")
                                   .append(parseInlineToHtml(label, accentColor)).append("</a>")
                                i = closeParen + 1
                                continue
                            }
                        }
                    }
                    out.append("[")
                    i += 1
                }
                else -> {
                    out.append(escapeHtml(text[i].toString()))
                    i++
                }
            }
        }
        return out.toString()
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;")
    }

    // Only allow safe link schemes to avoid javascript:/data: injection in exported HTML.
    private fun sanitizeUrl(url: String): String {
        val trimmed = url.trim()
        // Relative links and fragments/anchors are safe.
        if (trimmed.startsWith("#") || trimmed.startsWith("/") || trimmed.startsWith("./") || trimmed.startsWith("../")) {
            return trimmed
        }
        val lower = trimmed.lowercase()
        val allowed = listOf("http://", "https://", "mailto:", "tel:")
        return if (allowed.any { lower.startsWith(it) }) trimmed else "#"
    }

    private fun highlightHtmlCode(code: String, isDark: Boolean): String {
        // Always HTML-escape the code text; only the wrapping <span> tags are intentional markup.
        if (!isDark) return escapeHtml(code)
        val keywords = setOf("fun", "function", "val", "var", "class", "import", "return", "package", "override", "let", "const")
        val lines = code.split("\n")
        val highlighted = mutableListOf<String>()
        lines.forEach { line ->
            var i = 0
            val len = line.length
            val out = java.lang.StringBuilder()
            while (i < len) {
                if (line.startsWith("//", i) || line.startsWith("#", i)) {
                    out.append("<span class=\"comment\">").append(escapeHtml(line.substring(i))).append("</span>")
                    break
                } else if (line[i] == '"' || line[i] == '\'') {
                    val quote = line[i]
                    var end = i + 1
                    while (end < len && line[end] != quote) {
                        if (line[end] == '\\' && end + 1 < len) end += 2 else end++
                    }
                    if (end < len) end++
                    out.append("<span class=\"string\">").append(escapeHtml(line.substring(i, end))).append("</span>")
                    i = end
                } else if (line[i].isLetter()) {
                    var end = i
                    while (end < len && (line[end].isLetterOrDigit() || line[end] == '_')) {
                        end++
                    }
                    val word = line.substring(i, end)
                    if (word in keywords) {
                        out.append("<span class=\"keyword\">").append(escapeHtml(word)).append("</span>")
                    } else {
                        out.append(escapeHtml(word))
                    }
                    i = end
                } else {
                    out.append(escapeHtml(line[i].toString()))
                    i++
                }
            }
            highlighted.add(out.toString())
        }
        return highlighted.joinToString("\n")
    }

    fun exportHtml(context: Context, document: MarkdownDocument, preferences: ReaderPreferences) {
        val htmlContent = generateHtmlWithTheme(document, preferences)
        val fileName = "${document.title.replace("\\s+".toRegex(), "_")}.html"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/html")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { output ->
                        output?.write(htmlContent.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Archivo guardado en Descargas: $fileName", Toast.LENGTH_LONG).show()
                    shareFile(context, uri, "text/html", "Compartir documento HTML")
                } else {
                    throw Exception("No se pudo crear el archivo en Descargas")
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { output ->
                    output.write(htmlContent.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Archivo guardado en Descargas: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                val uri = try {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "com.aistudio.marked.readereditor.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    Uri.fromFile(file)
                }
                shareFile(context, uri, "text/html", "Compartir documento HTML")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Robust fallback: Save to internal cache directory and try sharing it from there
            try {
                val cacheDir = File(context.cacheDir, "exports")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val file = File(cacheDir, fileName)
                FileOutputStream(file).use { output ->
                    output.write(htmlContent.toByteArray(Charsets.UTF_8))
                }
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.aistudio.marked.readereditor.fileprovider",
                    file
                )
                Toast.makeText(context, "Guardado en caché de la app debido a restricciones de almacenamiento", Toast.LENGTH_LONG).show()
                shareFile(context, uri, "text/html", "Compartir documento HTML")
            } catch (ex: Exception) {
                ex.printStackTrace()
                Toast.makeText(context, "Error al guardar el HTML: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun printPdf(context: Context, document: MarkdownDocument, preferences: ReaderPreferences) {
        try {
            val htmlContent = generateHtmlWithTheme(document, preferences)
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = false
            }
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    try {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                        if (printManager != null) {
                            val jobName = "${document.title.replace("\\s+".toRegex(), "_")}_PDF_Export"
                            val printAdapter = webView.createPrintDocumentAdapter(jobName)
                            printManager.print(
                                jobName, 
                                printAdapter, 
                                PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .build()
                            )
                        } else {
                            Toast.makeText(context, "El servicio de impresión no está disponible en este dispositivo", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error al procesar la impresión: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            
            webView.loadDataWithBaseURL("https://local.reader/", htmlContent, "text/html", "utf-8", "https://local.reader/")
        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(context, "El servicio de impresión o componente WebView no está disponible", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(context: Context, uri: Uri, mimeType: String, title: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Documento Exportado")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No se pudo compartir el archivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
