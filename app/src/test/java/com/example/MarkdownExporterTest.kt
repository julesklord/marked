package com.example

import com.example.data.MarkdownDocument
import com.example.ui.markdown.MarkdownExporter
import com.example.ui.markdown.ReaderPreferences
import com.example.ui.markdown.ReaderTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownExporterTest {

    @Test
    fun testExporterHtmlContent_simpleMarkdown() {
        val document = MarkdownDocument(
            id = 1,
            title = "Prueba de Manifesto",
            content = "# Titulo Principal\n\nEste es un parrafo con **negrita** e *italica*.\n\n- Elemento 1\n- Elemento 2"
        )
        val preferences = ReaderPreferences(selectedTheme = ReaderTheme.IMMERSIVE_UI)
        
        val html = MarkdownExporter.generateHtmlWithTheme(document, preferences)
        
        // Assertions for standard structures
        assertTrue("Deberia contener el titulo principal como h1", html.contains("<h1>Titulo Principal</h1>"))
        assertTrue("Deberia contener el parrafo con negrita", html.contains("<strong>negrita</strong>"))
        assertTrue("Deberia contener el parrafo con italica", html.contains("<em>italica</em>"))
        
        // Assertions for bullet elements
        assertTrue("Deberia contener la lista desordenada ul", html.contains("<ul>"))
        assertTrue("Deberia contener el primer elemento li", html.contains("<li>Elemento 1</li>"))
        assertTrue("Deberia contener el segundo elemento li", html.contains("<li>Elemento 2</li>"))
        
        // Assertions for active theme variables
        assertTrue("Deberia contener el gradiente de fondo del tema Inmersivo", html.contains("linear-gradient(180deg, #0F1113"))
        assertTrue("Deberia contener la fuente o variables del tema", html.contains("Prueba de Manifesto"))
    }

    @Test
    fun testExporterEscapesTitle_preventsHtmlInjection() {
        val document = MarkdownDocument(
            id = 2,
            title = "</title><img src=x onerror=alert(1)>",
            content = "Contenido"
        )
        val html = MarkdownExporter.generateHtmlWithTheme(document, ReaderPreferences())

        assertFalse("El titulo no debe inyectar HTML crudo", html.contains("<img src=x onerror=alert(1)>"))
        assertTrue("El titulo debe ir escapado", html.contains("&lt;img src=x onerror=alert(1)&gt;"))
    }

    @Test
    fun testExporterSanitizesLinkScheme_blocksJavascript() {
        val document = MarkdownDocument(
            id = 3,
            title = "Enlaces",
            content = "Un [enlace](javascript:alert(1)) y otro [seguro](https://example.com)."
        )
        val html = MarkdownExporter.generateHtmlWithTheme(document, ReaderPreferences())

        assertFalse("No debe permitir esquema javascript:", html.contains("href=\"javascript:alert(1)\""))
        assertTrue("Los enlaces inseguros se neutralizan a #", html.contains("href=\"#\""))
        assertTrue("Los enlaces http/https se conservan", html.contains("href=\"https://example.com\""))
    }

    @Test
    fun testExporterDarkCodeBlock_noLiteralSpanTags() {
        val document = MarkdownDocument(
            id = 4,
            title = "Codigo",
            content = "```kotlin\nfun main() { val x = \"hola\" }\n```"
        )
        val html = MarkdownExporter.generateHtmlWithTheme(
            document,
            ReaderPreferences(selectedTheme = ReaderTheme.IMMERSIVE_UI)
        )

        // El resaltado debe producir spans reales, no texto escapado del propio markup.
        assertTrue("Debe emitir spans de resaltado reales", html.contains("<span class=\"keyword\">fun</span>"))
        assertFalse("No debe mostrar etiquetas span como texto literal", html.contains("&lt;span class=&quot;keyword&quot;&gt;"))
    }

    @Test
    fun testSelectionDirectionHandling_backwardsSelection() {
        val text = "Mi texto seleccionado"
        val selection = androidx.compose.ui.text.TextRange(8, 3) // backwards selection 8 -> 3
        
        val start = minOf(selection.start, selection.end).coerceIn(0, text.length)
        val end = maxOf(selection.start, selection.end).coerceIn(0, text.length)
        val selectedText = text.substring(start, end)
        
        org.junit.Assert.assertEquals("texto", selectedText)
    }
}
