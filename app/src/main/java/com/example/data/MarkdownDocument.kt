package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "markdown_documents")
data class MarkdownDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
