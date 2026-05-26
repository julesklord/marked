package com.example.ui.markdown

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class BulletList(val items: List<String>) : MarkdownBlock()
    data class OrderedList(val items: List<String>) : MarkdownBlock()
    data class Checklist(val items: List<ChecklistItem>) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

data class ChecklistItem(val isChecked: Boolean, val text: String, val lineIndex: Int)

object MarkdownParser {

    fun parse(rawText: String): List<MarkdownBlock> {
        if (rawText.isBlank()) return emptyList()

        val lines = rawText.split("\n")
        val blocks = mutableListOf<MarkdownBlock>()

        var inCodeBlock = false
        var codeLanguage = ""
        val codeBuffer = StringBuilder()

        var currentBulletList = mutableListOf<String>()
        var currentOrderedList = mutableListOf<String>()
        var currentChecklist = mutableListOf<ChecklistItem>()

        fun flushLists() {
            if (currentBulletList.isNotEmpty()) {
                blocks.add(MarkdownBlock.BulletList(currentBulletList.toList()))
                currentBulletList.clear()
            }
            if (currentOrderedList.isNotEmpty()) {
                blocks.add(MarkdownBlock.OrderedList(currentOrderedList.toList()))
                currentOrderedList.clear()
            }
            if (currentChecklist.isNotEmpty()) {
                blocks.add(MarkdownBlock.Checklist(currentChecklist.toList()))
                currentChecklist.clear()
            }
        }

        var i = 0
        while (i < lines.size) {
            val originalLine = lines[i]
            val line = originalLine.trim()

            // Handle Code Block
            if (line.startsWith("```")) {
                if (inCodeBlock) {
                    // End of code block
                    blocks.add(MarkdownBlock.CodeBlock(codeLanguage, codeBuffer.toString().trimEnd()))
                    codeBuffer.clear()
                    codeLanguage = ""
                    inCodeBlock = false
                } else {
                    // Start of code block
                    flushLists()
                    inCodeBlock = true
                    codeLanguage = line.removePrefix("```").trim()
                }
                i++
                continue
            }

            if (inCodeBlock) {
                codeBuffer.append(originalLine).append("\n")
                i++
                continue
            }

            // Empty lines split lists and add soft space
            if (line.isEmpty()) {
                flushLists()
                // Let's add a spacer block if needed or let paragraphs handle spacing.
                i++
                continue
            }

            // Horizontal rules
            if (line == "---" || line == "***" || line == "___") {
                flushLists()
                blocks.add(MarkdownBlock.Divider)
                i++
                continue
            }

            // Headers
            if (line.startsWith("#")) {
                flushLists()
                val headerMatch = Regex("^(#{1,6})\\s+(.*)$").find(line)
                if (headerMatch != null) {
                    val level = headerMatch.groupValues[1].length
                    val text = headerMatch.groupValues[2]
                    blocks.add(MarkdownBlock.Header(level, text))
                } else {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
                i++
                continue
            }

            // Blockquote
            if (line.startsWith(">")) {
                flushLists()
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    val cleaned = lines[i].trim().substring(1).trim()
                    quoteLines.add(cleaned)
                    i++
                }
                blocks.add(MarkdownBlock.Quote(quoteLines.joinToString("\n")))
                continue
            }

            // Interactive Checklist
            // - [ ] text, * [ ] text, - [x] text, * [x] text
            val checkMatch = Regex("^[-*]\\s+\\[([ xX])\\]\\s+(.*)$").find(line)
            if (checkMatch != null) {
                // Flush other lists if checklist started
                if (currentBulletList.isNotEmpty() || currentOrderedList.isNotEmpty()) {
                    flushLists()
                }
                val isChecked = checkMatch.groupValues[1].equals("x", ignoreCase = true)
                val text = checkMatch.groupValues[2]
                currentChecklist.add(ChecklistItem(isChecked, text, i))
                i++
                continue
            }

            // Unordered List (- item or * item)
            val bulletMatch = Regex("^[-*]\\s+(.*)$").find(line)
            if (bulletMatch != null) {
                if (currentOrderedList.isNotEmpty() || currentChecklist.isNotEmpty()) {
                    flushLists()
                }
                currentBulletList.add(bulletMatch.groupValues[1])
                i++
                continue
            }

            // Ordered List (1. item)
            val orderedMatch = Regex("^\\d+\\.\\s+(.*)$").find(line)
            if (orderedMatch != null) {
                if (currentBulletList.isNotEmpty() || currentChecklist.isNotEmpty()) {
                    flushLists()
                }
                currentOrderedList.add(orderedMatch.groupValues[1])
                i++
                continue
            }

            // Plain Paragraph text
            flushLists()
            // Aggregate consecutive text lines into one paragraph block
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size && 
                   lines[i].trim().isNotEmpty() && 
                   !lines[i].trim().startsWith("#") && 
                   !lines[i].trim().startsWith(">") && 
                   !lines[i].trim().startsWith("```") && 
                   !Regex("^[-*]\\s+(\\[[ xX]\\])?\\s+.*$").containsMatchIn(lines[i].trim()) &&
                   !Regex("^\\d+\\.\\s+.*$").containsMatchIn(lines[i].trim()) &&
                   lines[i].trim() != "---" && lines[i].trim() != "***" && lines[i].trim() != "___") {
                paragraphLines.add(lines[i].trim())
                i++
            }
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString(" ")))
        }

        flushLists()
        return blocks
    }
}
