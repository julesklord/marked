package com.example

import com.example.ui.markdown.MarkdownBlock
import com.example.ui.markdown.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun parsesHeadersWithLevels() {
        val blocks = MarkdownParser.parse("# Uno\n### Tres")
        assertEquals(2, blocks.size)
        val h1 = blocks[0] as MarkdownBlock.Header
        val h3 = blocks[1] as MarkdownBlock.Header
        assertEquals(1, h1.level)
        assertEquals("Uno", h1.text)
        assertEquals(3, h3.level)
        assertEquals("Tres", h3.text)
    }

    @Test
    fun headerKeepsInlineMarkupAsRawText() {
        // The parser keeps inline tokens; inline formatting is applied at render time.
        val blocks = MarkdownParser.parse("# Hola **mundo** y `code`")
        val header = blocks.single() as MarkdownBlock.Header
        assertEquals("Hola **mundo** y `code`", header.text)
    }

    @Test
    fun aggregatesConsecutiveLinesIntoOneParagraph() {
        val blocks = MarkdownParser.parse("linea uno\nlinea dos")
        val paragraph = blocks.single() as MarkdownBlock.Paragraph
        assertEquals("linea uno linea dos", paragraph.text)
    }

    @Test
    fun groupsBulletAndOrderedLists() {
        val bullets = MarkdownParser.parse("- a\n- b\n* c")
        val bulletList = bullets.single() as MarkdownBlock.BulletList
        assertEquals(listOf("a", "b", "c"), bulletList.items)

        val ordered = MarkdownParser.parse("1. uno\n2. dos")
        val orderedList = ordered.single() as MarkdownBlock.OrderedList
        assertEquals(listOf("uno", "dos"), orderedList.items)
    }

    @Test
    fun parsesChecklistWithStateAndLineIndex() {
        val blocks = MarkdownParser.parse("- [ ] a\n- [x] b\n- [X] c")
        val checklist = blocks.single() as MarkdownBlock.Checklist
        assertEquals(3, checklist.items.size)

        assertEquals(false, checklist.items[0].isChecked)
        assertEquals("a", checklist.items[0].text)
        assertEquals(0, checklist.items[0].lineIndex)

        assertTrue("[x] debe quedar marcado", checklist.items[1].isChecked)
        assertEquals(1, checklist.items[1].lineIndex)

        assertTrue("[X] mayuscula tambien marca", checklist.items[2].isChecked)
        assertEquals(2, checklist.items[2].lineIndex)
    }

    @Test
    fun parsesCodeBlockLanguageAndContent() {
        val blocks = MarkdownParser.parse("```kotlin\nval x = 1\nval y = 2\n```")
        val code = blocks.single() as MarkdownBlock.CodeBlock
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1\nval y = 2", code.code)
    }

    @Test
    fun parsesMultilineBlockquote() {
        val blocks = MarkdownParser.parse("> primera\n> segunda")
        val quote = blocks.single() as MarkdownBlock.Quote
        assertEquals("primera\nsegunda", quote.text)
    }

    @Test
    fun parsesDividers() {
        listOf("---", "***", "___").forEach { token ->
            val blocks = MarkdownParser.parse(token)
            assertTrue("$token deberia ser un Divider", blocks.single() is MarkdownBlock.Divider)
        }
    }

    @Test
    fun blankInputProducesNoBlocks() {
        assertTrue(MarkdownParser.parse("   \n  ").isEmpty())
    }
}
