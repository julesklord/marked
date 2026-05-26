package com.example

import com.example.data.MarkdownDocument
import com.example.ui.markdown.MarkdownExporter
import com.example.ui.markdown.ReaderPreferences
import com.example.ui.markdown.ReaderTheme
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
    fun testSelectionDirectionHandling_backwardsSelection() {
        val text = "Mi texto seleccionado"
        val selection = androidx.compose.ui.text.TextRange(8, 3) // backwards selection 8 -> 3
        
        val start = minOf(selection.start, selection.end).coerceIn(0, text.length)
        val end = maxOf(selection.start, selection.end).coerceIn(0, text.length)
        val selectedText = text.substring(start, end)
        
        org.junit.Assert.assertEquals("texto", selectedText)
    }
}
