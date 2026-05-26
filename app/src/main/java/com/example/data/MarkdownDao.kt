package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkdownDao {
    @Query("SELECT * FROM markdown_documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<MarkdownDocument>>

    @Query("SELECT * FROM markdown_documents WHERE id = :id LIMIT 1")
    fun getDocumentById(id: Int): Flow<MarkdownDocument?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: MarkdownDocument): Long

    @Update
    suspend fun updateDocument(document: MarkdownDocument)

    @Delete
    suspend fun deleteDocument(document: MarkdownDocument)

    @Query("DELETE FROM markdown_documents")
    suspend fun deleteAll()
}
