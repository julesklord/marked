package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MarkdownDocument
import com.example.data.MarkdownRepository
import com.example.ui.markdown.ReaderPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MarkdownViewModel(private val repository: MarkdownRepository) : ViewModel() {

    val allDocuments: StateFlow<List<MarkdownDocument>> = repository.allDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDocument = MutableStateFlow<MarkdownDocument?>(null)
    val selectedDocument: StateFlow<MarkdownDocument?> = _selectedDocument.asStateFlow()

    private val _readerPreferences = MutableStateFlow(ReaderPreferences())
    val readerPreferences: StateFlow<ReaderPreferences> = _readerPreferences.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Observe documents and auto-preload welcome files if database is empty
        viewModelScope.launch {
            allDocuments.collectLatest { docs ->
                if (docs.isEmpty()) {
                    createDefaultDocumentTemplates()
                } else if (_selectedDocument.value == null) {
                    // Default to first document on startup
                    _selectedDocument.value = docs.first()
                } else {
                    // Sync selected document with updated room content (e.g. name rename, check progress)
                    val currentId = _selectedDocument.value?.id
                    val freshDoc = docs.find { it.id == currentId }
                    if (freshDoc != null) {
                        _selectedDocument.value = freshDoc
                    }
                }
            }
        }
    }

    private suspend fun createDefaultDocumentTemplates() {
        val welcomeDoc = MarkdownDocument(
            title = "¡Bienvenido a Marked! 📑",
            content = """# ¡Bienvenido a Marked! 📑

**Marked** es un editor y lector ligero de alto rendimiento diseñado exclusivamente para Android. Aquí puedes crear, organizar y leer tus notas en formato **Markdown** con una experiencia tipográfica con altos estándares de diseño.

### Características Principales:
- ⚡ **Lector de Alta Calidad**: Renderizado responsivo impecable para todos tus dispositivos.
- 🎨 **Fuentes Configurables**: Ajusta el tamaño de fuente, interlineado y estilo tipográfico al instante.
- 🌓 **Modo Oscuro Nativo**: Elije entre varios temas seleccionados (Papel, Sepia, Carbono o Negro Absoluto) para proteger tu vista.
- ☑️ **Listas de Tareas Interactivas**: Toca las casillas de verificación directamente desde la vista del lector para tachar tareas.

> Puedes abrir el menú lateral deslizando o tocando el botón de carpeta para crear o renombrar tus notas.

---
Creado con cariño por Marked Team.
""".trimIndent()
        )

        val cheatsheetDoc = MarkdownDocument(
            title = "Guía de Sintaxis 🖋️",
            content = """# Guía de Sintaxis Markdown 🖋️

Esta nota te ayuda a explorar las opciones de formato enriquecido que soporta nuestro lector de alta calidad.

## Cabeceras
Puedes usar cabeceras del nivel 1 al nivel 4:
### Cabecera Nivel 3 (Muy elegante)
#### Cabecera Nivel 4 (Compacta y bonita)

---

## Formato de Texto
- Puedes escribir en **negrita** para resaltar ideas importantes.
- Puedes escribir en *itálica* para dar énfasis.
- También puedes usar `código en línea` para variables o etiquetas breves.
- O añadir hipervínculos como [Google](https://google.com).

---

## Citas y Pensamientos
> "El buen diseño se nota en los detalles de tipografía y espaciado." — Equipo de Diseño

---

## Bloques de Código
Aquí tienes un ejemplo de código en Kotlin:
```kotlin
fun main() {
    println("¡Hola desde Marked!")
}
```
""".trimIndent()
        )

        val checklistDoc = MarkdownDocument(
            title = "Mis Tareas 🚀",
            content = """# Proyecto: Nueva Aplicación 🚀

Usa esta lista de verificación interactiva para organizar tus planes de desarrollo. ¡Puedes tocar las casillas directamente desde el lector y ver cómo se actualiza el documento!

## Tareas de Diseño
- [x] Elegir una tipografía elegante para lectura
- [x] Configurar modo oscuro y temas de fondo (Papel y Sepia)
- [ ] Refinar las sombras y bordes de Material 3

## Tareas de Código
- [x] Configurar la base de datos local Room
- [ ] Implementar el editor de texto interactivo con barra de herramientas
- [ ] Añadir botón para copiar bloques de código al Portapapeles
""".trimIndent()
        )

        repository.saveDocument(welcomeDoc)
        repository.saveDocument(cheatsheetDoc)
        repository.saveDocument(checklistDoc)
    }

    fun selectDocument(document: MarkdownDocument) {
        _selectedDocument.value = document
    }

    fun setEditMode(editMode: Boolean) {
        _isEditMode.value = editMode
    }

    fun updatePreferences(update: (ReaderPreferences) -> ReaderPreferences) {
        _readerPreferences.value = update(_readerPreferences.value)
    }

    fun createNewDocument(title: String) {
        viewModelScope.launch {
            val emptyDoc = MarkdownDocument(
                title = title.ifBlank { "Sin Título" },
                content = "# ${title.ifBlank { "Sin Título" }}\n\nEscribe aquí tu contenido markdown..."
            )
            val insertedId = repository.saveDocument(emptyDoc)
            val addedDoc = emptyDoc.copy(id = insertedId.toInt())
            _selectedDocument.value = addedDoc
            _isEditMode.value = true // Open in edit mode immediately for convenience
        }
    }

    fun updateDocumentContent(content: String) {
        val current = _selectedDocument.value ?: return
        viewModelScope.launch {
            val updated = current.copy(content = content, updatedAt = System.currentTimeMillis())
            repository.updateDocument(updated)
            _selectedDocument.value = updated
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
                title = newTitle.ifBlank { "Sin Título" },
                content = currentContent,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateDocument(updated)
            if (_selectedDocument.value?.id == documentId) {
                _selectedDocument.value = updated
            }
        }
    }

    fun deleteDocument(document: MarkdownDocument) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            if (_selectedDocument.value?.id == document.id) {
                _selectedDocument.value = allDocuments.value.find { it.id != document.id }
            }
        }
    }

    fun toggleChecklistItem(lineIndex: Int) {
        val currentDoc = _selectedDocument.value ?: return
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
                _selectedDocument.value = updated
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

class MarkdownViewModelFactory(private val repository: MarkdownRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkdownViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarkdownViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
