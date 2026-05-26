package com.example.data

import kotlinx.coroutines.flow.Flow

class MarkdownRepository(private val markdownDao: MarkdownDao) {
    val allDocuments: Flow<List<MarkdownDocument>> = markdownDao.getAllDocuments()

    fun getDocumentById(id: Int): Flow<MarkdownDocument?> {
        return markdownDao.getDocumentById(id)
    }

    suspend fun saveDocument(document: MarkdownDocument): Long {
        return markdownDao.insertDocument(document)
    }

    suspend fun updateDocument(document: MarkdownDocument) {
        markdownDao.updateDocument(document)
    }

    suspend fun deleteDocument(document: MarkdownDocument) {
        markdownDao.deleteDocument(document)
    }
}
