package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.MarkdownDocument
import com.example.data.MarkdownRepository
import com.example.ui.markdown.ReaderPreferences
import com.example.ui.markdown.ReaderPreferencesStore
import com.example.ui.markdown.ReaderTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MarkdownViewModel(
    private val repository: MarkdownRepository,
    private val preferencesStore: ReaderPreferencesStore,
    private val context: Context
) : ViewModel() {

    val allDocuments: StateFlow<List<MarkdownDocument>> = repository.allDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDocumentId = MutableStateFlow<Int?>(null)
    val selectedDocument: StateFlow<MarkdownDocument?> = combine(
        allDocuments,
        _selectedDocumentId
    ) { docs, id ->
        if (id == null) {
            docs.firstOrNull()
        } else {
            docs.find { it.id == id } ?: docs.firstOrNull()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val readerPreferences: StateFlow<ReaderPreferences> = preferencesStore.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReaderPreferences()
        )

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Pre-load default template notes only once on startup if DB is completely empty
        viewModelScope.launch {
            if (repository.allDocuments.first().isEmpty()) {
                createDefaultDocumentTemplates()
            }
        }
    }

    private suspend fun createDefaultDocumentTemplates() {
        val welcomeDoc = MarkdownDocument(
            title = context.getString(R.string.template_welcome_title),
            content = context.getString(R.string.template_welcome_content)
        )

        val cheatsheetDoc = MarkdownDocument(
            title = context.getString(R.string.template_syntax_title),
            content = context.getString(R.string.template_syntax_content)
        )

        val checklistDoc = MarkdownDocument(
            title = context.getString(R.string.template_tasks_title),
            content = context.getString(R.string.template_tasks_content)
        )

        repository.saveDocument(welcomeDoc)
        repository.saveDocument(cheatsheetDoc)
        repository.saveDocument(checklistDoc)
    }

    fun selectDocument(document: MarkdownDocument) {
        _selectedDocumentId.value = document.id
    }

    fun setEditMode(editMode: Boolean) {
        _isEditMode.value = editMode
    }

    fun updatePreferences(update: (ReaderPreferences) -> ReaderPreferences) {
        viewModelScope.launch {
            preferencesStore.save(update(readerPreferences.value))
        }
    }

    /**
     * On the very first launch (no preferences persisted yet) pick a sensible default theme based on
     * the system dark mode. Once the user has any saved preference this is a no-op so their choice
     * survives restarts.
     */
    fun applyDefaultThemeIfFirstLaunch(systemInDark: Boolean) {
        viewModelScope.launch {
            if (!preferencesStore.isInitializedFlow.first()) {
                val defaultTheme = if (systemInDark) ReaderTheme.IMMERSIVE_UI else ReaderTheme.PAPELES
                preferencesStore.save(readerPreferences.value.copy(selectedTheme = defaultTheme))
            }
        }
    }

    fun createNewDocument(title: String) {
        viewModelScope.launch {
            val defaultTitle = context.getString(R.string.untitled_doc)
            val placeholderContent = context.getString(R.string.editor_placeholder)
            val emptyDoc = MarkdownDocument(
                title = title.ifBlank { defaultTitle },
                content = "# ${title.ifBlank { defaultTitle }}\n\n$placeholderContent"
            )
            val insertedId = repository.saveDocument(emptyDoc)
            _selectedDocumentId.value = insertedId.toInt()
            _isEditMode.value = true // Open in edit mode immediately for convenience
        }
    }

    fun updateDocumentContent(content: String) {
        val current = selectedDocument.value ?: return
        viewModelScope.launch {
            val updated = current.copy(content = content, updatedAt = System.currentTimeMillis())
            repository.updateDocument(updated)
        }
    }

    fun renameDocument(documentId: Int, newTitle: String) {
        viewModelScope.launch {
            val doc = allDocuments.value.find { it.id == documentId } ?: return@launch
            // Also update raw content title header if possible
            var currentContent = doc.content
            if (currentContent.startsWith("# ")) {
                val lines = currentContent.split("\n").toMutableList()
                if (lines.isNotEmpty() && lines[0].startsWith("# ")) {
                    lines[0] = "# $newTitle"
                    currentContent = lines.joinToString("\n")
                }
            }

            val updated = doc.copy(
                title = newTitle.ifBlank { context.getString(R.string.untitled_doc) },
                content = currentContent,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateDocument(updated)
        }
    }

    fun deleteDocument(document: MarkdownDocument) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            if (_selectedDocumentId.value == document.id) {
                _selectedDocumentId.value = null
            }
        }
    }

    fun toggleChecklistItem(lineIndex: Int) {
        val currentDoc = selectedDocument.value ?: return
        viewModelScope.launch {
            val lines = currentDoc.content.split("\n").toMutableList()
            if (lineIndex >= 0 && lineIndex < lines.size) {
                val line = lines[lineIndex]
                val newLine = when {
                    line.contains("[ ]") -> line.replace("[ ]", "[x]")
                    line.contains("[x]") -> line.replace("[x]", "[ ]")
                    line.contains("[X]") -> line.replace("[X]", "[ ]")
                    else -> line
                }
                lines[lineIndex] = newLine
                val updated = currentDoc.copy(
                    content = lines.joinToString("\n"),
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateDocument(updated)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

class MarkdownViewModelFactory(
    private val repository: MarkdownRepository,
    private val preferencesStore: ReaderPreferencesStore,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkdownViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarkdownViewModel(repository, preferencesStore, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
