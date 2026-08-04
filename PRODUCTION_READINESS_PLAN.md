# Plan de Implementación: Marked - Puesta a Punto para Producción

> **Fecha**: 2026-06-08  
> **Estado Actual**: MVP funcional - Necesita refactorización mayor  
> **Objetivo**: Transformar Marked en una aplicación Android enterprise-grade lista para distribución en Google Play  

---

## Índice

1. [Análisis de la Arquitectura Actual](#1-análisis-de-la-arquitectura-actual)
2. [Fase 1: Refactorización Arquitectónica](#2-fase-1-refactorización-arquitectónica)
3. [Fase 2: Inyección de Dependencias con Hilt](#3-fase-2-inyección-de-dependencias-con-hilt)
4. [Fase 3: Rendimiento y Estabilidad](#4-fase-3-rendimiento-y-estabilidad)
5. [Fase 4: Testing Exhaustivo](#5-fase-4-testing-exhaustivo)
6. [Fase 5: Manejo de Errores y Logging](#6-fase-5-manejo-de-errores-y-logging)
7. [Fase 6: Optimización UI/UX](#7-fase-6-optimización-uiux)
8. [Fase 7: Preparación para Release](#8-fase-7-preparación-para-release)
9. [Cronograma de Implementación](#9-cronograma-de-implementación)
10. [Checklist de Validación](#10-checklist-de-validación)

---

## 1. Análisis de la Arquitectura Actual

### 1.1 Estado Actual de los Componentes

```
┌─────────────────────────────────────────────────────────────────┐
│                        MAIN ACTIVITY                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │   Scaffold  │  │   TopBar    │  │   Settings Dialog       │ │
│  │   (819 L)   │  │  (embedded) │  │   (embedded - 300 L)    │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │  Sidebar    │  │  EditorArea │  │  Document Create/Rename/│ │
│  │  (embedded) │  │ (embedded)  │  │  Delete Dialogs         │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                     ┌────────▼────────┐
                     │   ViewModel      │
                     │ (492 L - grande) │
                     └────────┬─────────┘
                     ┌────────▼────────┐
                     │   Repository     │
                     │ (23 L - proxy)   │
                     └────────┬─────────┘
                     ┌────────▼────────┐
                     │      Room DAO     │
                     └─────────────────┘
```

### 1.2 Métricas de Complejidad

| Archivo         | Líneas  | Responsabilidades                   | Problema Principal |
|-----------------|---------|-------------------------------------|--------------------|
| MainActivity.kt | 1,297   | UI, scaffold, dialogs, nav, themes  | God Class, SRP violado |
| MarkdownRenderer.kt | 715 | Parsing + Rendering + Styling      | Acoplamiento parse-render |
| MarkdownExporter.kt | 547 | HTML + PDF + Share + UI messages   | Duplicación de sanitización |
| MarkdownViewModel.kt | 208 | State mgmt + Business logic + Init | Sin capa de dominio |

---

## 2. Fase 1: Refactorización Arquitectónica

### 2.1 Arquitectura Objetivo: Clean Architecture (MVVM + Use Cases)

```
app/src/main/java/com/aistudio/marked/
│
├── core/                          # Código transversal
│   ├── util/                      # Extensiones funciones, DateUtils, etc.
│   ├── security/                  # Sanitización, validación de URLs
│   └── di/                        # Módulos de inyección de dependencias
│
├── domain/                        # CAPA DE DOMINIO (Reglas de negocio puras)
│   ├── model/                     # Entities, Value Objects (sin Android/Jetpack)
│   │   ├── Document.kt
│   │   ├── MarkdownBlock.kt
│   │   ├── ReaderPreferences.kt
│   │   └── ReaderTheme.kt
│   │
│   ├── repository/                # Interfaces (puertos)
│   │   ├── DocumentRepository.kt
│   │   └── PreferencesRepository.kt
│   │
│   └── usecase/                   # Casos de uso (operaciones atómicas)
│       ├── CreateDocumentUseCase.kt
│       ├── UpdateDocumentUseCase.kt
│       ├── DeleteDocumentUseCase.kt
│       ├── GetDocumentsUseCase.kt
│       ├── ExportDocumentUseCase.kt
│       └── ToggleChecklistUseCase.kt
│
├── data/                          # CAPA DE DATOS (adaptadores)
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── MarkdownDao.kt
│   │   │   └── MarkdownDocumentEntity.kt
│   │   └── ds/
│   │       └── PreferencesDataStore.kt
│   │
│   ├── mapper/                    # Mappeo Domain <-> Entity
│   │   ├── DocumentMapper.kt
│   │   └── PreferencesMapper.kt
│   │
│   └── repository/                # Implementaciones concretas
│       ├── DocumentRepositoryImpl.kt
│       └── PreferencesRepositoryImpl.kt
│
├── markdown/                      # MOTOR DE MARKDOWN (standalone)
│   ├── parser/
│   │   ├── MarkdownParser.kt      # Pure Kotlin (sin Compose)
│   │   ├── MarkdownBlock.kt       # AST del documento
│   │   └── InlineParser.kt        # Bold, italic, links, code inline
│   │
│   ├── renderer/
│   │   ├── ComposeRenderer.kt     # Renderizado a Compose (interfaz)
│   │   ├── ThemeColors.kt         # Mapeo de colores por tema
│   │   └── renderer-blocks/       # Componentes por tipo de bloque
│   │       ├── HeaderRenderer.kt
│   │       ├── CodeBlockRenderer.kt
│   │       ├── ChecklistRenderer.kt
│   │       └── ...
│   │
│   └── export/
│       ├── HtmlExporter.kt        # Generador de HTML (sin Android UI)
│       ├── Pdf技ная export/
│       └── PdfExporter.kt         # Generación de PDF
│
├── ui/                            # CAPA DE PRESENTACIÓN (Jetpack Compose)
│   ├── theme/                     # Temas y colores Material3
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   │
│   ├── scaffold/                  # Layout principal unificado
│   │   └── AppScaffold.kt         # Sidebar + Content + TopBar
│   │
│   ├── screens/                   # Pantallas principales
│   │   ├── reader/                # Modo lectura (reading mode)
│   │   │   ├── ReaderScreen.kt
│   │   │   └── ReaderViewModel.kt  # ViewModel específico
│   │   │
│   │   ├── editor/                # Modo edición (editing mode)
│   │   │   ├── EditorScreen.kt
│   │   │   └── EditorViewModel.kt
│   │   │
│   │   └── settings/              # Diálogo de configuración
│   │       ├── PreferencesDialog.kt
│   │       └── PreferencesViewModel.kt
│   │
│   ├── components/                # Componentes reutilizables
│   │   ├── sidebar/               
│   │   │   ├── SidebarContent.kt
│   │   │   ├── SearchBar.kt
│   │   │   └── DocumentListItem.kt
│   │   │
│   │   ├── dialogs/               # Diálogos reutilizables
│   │   │   ├── CreateDocumentDialog.kt
│   │   │   ├── RenameDocumentDialog.kt
│   │   │   └── DeleteDocumentDialog.kt
│   │   │
│   │   └── common/                # Componentes genéricos
│   │       ├── LoadingIndicator.kt
│   │       └── ErrorMessage.kt
│   │
│   └── viewmodel/                 # ViewModels compartidos
│       └── SharedViewModel.kt      # Para compartir estado entre   AppViewModel.kt      # Coordina reader/editor/settings
│       └── DocumentSelectionViewModel.kt
│
└── MainActivity.kt                # Entrada única (máx. 80 líneas)
```

### 2.2 Refactorización de MainActivity.kt (1,297 -> ~80 líneas)

```kotlin
// MainActivity.kt objetivo - Punto de entrada mínimo
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val viewModel: AppViewModel by viewModels { viewModelFactory }
        
        setContent {
            MarkedTheme {
                AppScaffold(viewModel = viewModel)
            }
        }
    }
}
```

### 2.3 Separación del MarkdownRenderer (715 -> múltiples archivos)

| Responsabilidad | Archivo Objetivo | Líneas Aprox. |
|-------------------|-------------------|---------------|
| Parseo de Markdown | `MarkdownParser.kt` | 150 |
| Renderizado de Headers | `HeaderRenderer.kt` | 80 |
| Renderizado de Code Blocks | `CodeBlockRenderer.kt` | 120 |
| Renderizado de Checklists | `ChecklistRenderer.kt` | 70 |
| Renderizado de Blockquotes | `BlockquoteRenderer.kt` | 60 |
| Highlighting de código | `SyntaxHighlighter.kt` | 180 |
| Parseo de inline text | `InlineTextParser.kt` | 120 |

### 2.4 Sepración de MarkdownExporter (547 -> múltiples clases)

```kotlin
// Interfaz del exportador
interface DocumentExporter {
    suspend fun export(document: Document, format: ExportFormat): Result<ExportOutput>
}

// Implementaciones concretas
class HtmlDocumentExporter(private val htmlGenerator: HtmlGenerator) : DocumentExporter
class PdfDocumentExporter(private val printManager: PrintManager) : DocumentExporter

// Generador de HTML (sin dependencias de Android UI)
class HtmlGenerator(private val themeProvider: ThemeProvider) {
    fun generate(document: Document, theme: ThemeConfig): String
}
```

---

## 3. Fase 2: Inyección de Dependencias con Hilt

### 3.1 Módulos de Inyección Necesarios

```kotlin
// Module: DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "marked_database")
            .build()
    }
    
    @Provides
    fun provideDao(database: AppDatabase): MarkdownDao = database.markdownDao()
    
    @Singleton
    @Provides
    fun provideDocumentRepository(dao: MarkdownDao): DocumentRepository {
        return DocumentRepositoryImpl(dao)
    }
}

// Module: PreferencesModule.kt
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    
    @Singleton
    @Provides
    fun providePreferencesDatastore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.dataStoreFile("reader_preferences")
        }
    }
    
    @Provides
    fun providePreferencesRepository(dataStore: DataStore<Preferences>): PreferencesRepository {
        return PreferencesRepositoryImpl(dataStore)
    }
}
```

### 3.2 Dependencias a Inyectar

| Componente | Dependencias a Inyectar |
|------------|------------------------|
| `AppViewModel` | `GetDocumentsUseCase`, `CreateDocumentUseCase`, `UpdateDocumentUseCase`, `DeleteDocumentUseCase`, `ExportDocumentUseCase`, `PreferencesRepository` |
| `ReaderScreen` | `ReaderViewModel` |
| `EditorScreen` | `EditorViewModel` |
| `DocumentRepository` | `MarkdownDao` |
| `PreferencesRepository` | `DataStore<Preferences>` |
| `MarkdownExporter` | `HtmlGenerator`, `PdfGenerator` |

---

## 4. Fase 3: Rendimiento y Estabilidad

### 4.1 Optimizaciones de Rendimiento

#### a) Cacheo de Parseo de Markdown

```kotlin
// PROBLEMA ACTUAL: Se parsea en cada recomposición
val blocks = MarkdownParser.parse(content) // Dentro de un Composable

// SOLUCIÓN: Cachear con derivedStateOf o ViewModel
class ReaderViewModel : ViewModel() {
    private val _document = MutableStateFlow<Document?>(null)
    val document: StateFlow<Document?> = _document.asStateFlow()
    
    // Cacheo del parseo - solo cuando cambia el contenido
    val parsedBlocks: StateFlow<List<MarkdownBlock>> = _document
        .map { it?.content?.let(MarkdownParser::parse) ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

#### b) Lazy Loading para Documentos Largos

```kotlin
@Composable
fun MarkdownRenderer(content: String, preferences: ReaderPreferences) {
    // PROBLEMA ACTUAL: Renderizado de todo el documento en Column simple
    
    // SOLUCIÓN: Usar LazyColumn para renderizado incremental
    LazyColumn {
        items(blocks, key = { it.hashCode() }) { block ->
            MarkdownBlockItem(block = block, preferences = preferences)
        }
    }
}
```

#### c) Memoización de Componentes Pesados

```kotlin
@Composable
fun CodeBlockRenderer(code: String, language: String, theme: Theme) {
    // Memoizar el resaltado de sintaxis
    val highlightedCode = remember(code, language) {
        SyntaxHighlighter.highlight(code, language)
    }
    
    // Memoizar el scroll state
    val scrollState = rememberScrollState()
    
    SelectionContainer {
        Box(modifier = Modifier.horizontalScroll(scrollState)) {
            Text(text = highlightedCode)
        }
    }
}
```

#### d) Debounce para Auto-guardado

```kotlin
class EditorViewModel(private val updateUseCase: UpdateDocumentUseCase) : ViewModel() {
    
    private val _contentChanges = MutableStateFlow("")
    
    init {
        _contentChanges
            .debounce(1500) // Guardar 1.5s después de dejar de escribir
            .onEach { content ->
                updateUseCase(content)
            }
            .launchIn(viewModelScope)
    }
    
    fun onContentChange(newContent: String) {
        _contentChanges.value = newContent
    }
}
```

### 4.2 Estabilidad: Transacciones DB

```kotlin
// PROBLEMA: Sin transacciones en operaciones críticas
suspend fun toggleChecklistItem(lineIndex: Int) {
    val currentDoc = selectedDocument.value ?: return
    // ... lógica sin transacción ...
    repository.updateDocument(updated) // ¿Qué pasa si falla?
}

// SOLUCIÓN: Usar @Transaction y Result<> para manejo de errores
@Transaction
suspend fun updateDocumentWithResult(document: Document): Result<Document> {
    return try {
        val result = withContext(Dispatchers.IO) {
            dao.update(document.toEntity())
        }
        Result.success(document)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 4.3 Configuración de ProGuard

```proguard
# app/proguard-rules.pro (actualizado para release)

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# DataStore
-keepclassmembers class * {
    @androidx.datastore.preferences.core.Preferences$Key <fields>;
}

# Moshi (si se usa)
-keep class * extends com.squareup.moshi.JsonAdapter
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Compose
-keepclassmembers class androidx.compose.runtime.ComposableKt { *; }

# Logging en Release (remover en producción)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
```

---

## 5. Fase 4: Testing Exhaustivo

### 5.1 Estrategia de Testing

```
┌─────────────────────────────────────────────────────────────┐
│                    PIRÁMIDE DE TESTING                        │
│                                                              │
│                    ┌───────────────┐                        │
│                    │   UI Tests    │  10% (E2E)              │
│                    │  (Compose UI) │  → Maestro/Espresso      │
│                    └───────┬───────┘                        │
│              ┌───────────┴───────────┐                      │
│              │   Integration Tests  │  20%                  │
│              │  (Repos + Use Cases)  │  → Hilt Testing       │
│              └煤电�─────────────────────┘                      │
│    ┌────────┴─────────┬───────────┴────────┐             │
│    │   Unit Tests    │  70% (enfásis)      │             │
│    │  (Pure Functions)│  → MockK + Turbine  │             │
│    └───────────────────┴──────────────────────┘             │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Tests de Unidad (Nuevos)

#### Test de MarkdownParser (expandido)

```kotlin
class MarkdownParserTest {

    @Test
    fun `given nested markdown syntax, parses correctly`() {
        val input = "**bold *italic* bold**"
        val result = MarkdownParser.parseInlineText(input)
        
        // Verificar que se aplican los estilos anidados
        assertTrue(result.hasStyle(SpanStyle(fontWeight = FontWeight.Bold)))
    }

    @Test
    fun `given empty checklist with no space, does not crash`() {
        val input = "-[] item" // Sin espacio entre - y []
        assertDoesNotThrow { MarkdownParser.parse(input) }
    }

    @Test
    fun `given unclosed code block, handles gracefully`() {
        val input = "```kotlin\nfun main() {\n// No cerrado"
        val result = MarkdownParser.parse(input)
        assertTrue(result.isNotEmpty())
    }
    
    @Test
    fun `given very long document, parses within time limit`() {
        val input = List(10000) { "Line $it" }.joinToString("\n")
        val time = measureTimeMillis { MarkdownParser.parse(input) }
        assertTrue(time < 1000) // Menos de 1 segundo
    }
}
```

#### Tests de ViewModel (nuevos)

```kotlin
class ReaderViewModelTest {

    @get:Rule val rule = InstantTaskExecutorRule()
    @get:Rule val coroutineRule = MainDispatcherRule()

    private lateinit var viewModel: ReaderViewModel
    private val getDocumentsUseCase = mockk<GetDocumentsUseCase>()
    private val exportUseCase = mockk<ExportDocumentUseCase>()
    
    @Before
    fun setup() {
        every { getDocumentsUseCase() } returns flowOf(listOf(Document(id = 1, title = "Test")))
        viewModel = ReaderViewModel(getDocumentsUseCase, exportUseCase)
    }

    @Test
    fun `select document updates state`() = runTest {
        val document = Document(id = 1, title = "Test")
        viewModel.selectDocument(document)
        
        viewModel.selectedDocument.test {
            assertEquals(document, awaitItem())
        }
    }

    @Test
    fun `toggle checklist item triggers use case`() = runTest {
        val toggleUseCase = mockk<ToggleChecklistUseCase>(relaxed = true)
        viewModel = ReaderViewModel(getDocumentsUseCase, toggleUseCase)
        
        viewModel.toggleChecklistItem(5)
        
        coVerify { toggleUseCase(5) }
    }
}
```

#### Tests de Repositorios (nuevos)

```kotlin
class DocumentRepositoryImplTest {

    @get:Rule val roomRule = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    ).build()
    
    private lateinit var repository: DocumentRepositoryImpl
    private val dao = roomRule.markdownDao()
    
    @Before
    fun setup() {
        repository = DocumentRepositoryImpl(dao)
    }

    @Test
    fun `save document returns id`() = runTest {
        val document = Document(title = "Test", content = "Body")
        val id = repository.save(document)
        
        assertTrue(id > 0)
    }

    @Test
    fun `get documents emits list ordered by updatedAt`() = runTest {
        repository.save(Document(title = "Old", updatedAt = 1000))
        repository.save(Document(title = "New", updatedAt = 2000))
        
        repository.getAll().test {
            val items = awaitItem()
            assertEquals("New", items[0].title)
            assertEquals("Old", items[1].title)
        }
    }
}
```

### 5.3 Tests de UI (Espresso / Compose UI Test)

```kotlin
class ReaderScreenTest {

    @get:Rule val composeTestRule = createComposeRule()
    
    @Test
    fun `reader screen displays payroll checklist`() {
        composeTestRule.setContent {
            val viewModel = ReaderViewModel.preview() // Factory para previews/ testing
            ReaderScreen(viewModel = viewModel)
        }
        
        composeTestRule.onNodeWithText("My Tasks 🚀").assertIsDisplayed()
        composeTestRule.onNodeWithTag("checklist_item_0").performClick()
        
        // Verificar que se marca como completado
        composeTestRule.onNodeWithTag("checklist_item_0").assertContentDescriptionContains("checked")
    }

    @Test
    fun `export dialogs work correctly`() {
        composeTestRule.setContent { ReaderScreen() }
        
        composeTestRule.onNodeWithContentDescription("Options").performClick()
        composeTestRule.onNodeWithText("Export as HTML").performClick()
        
        // Verificar que se abre el diálogo de exportación
        composeTestRule.onNodeWithText("Exporting...").assertIsDisplayed()
    }
}
```

### 5.4 Tests de Screenshot (Roborazzi - ya implementado, ampliar)

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ReaderScreenshotsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `renders all markdown blocks correctly`() {
        composeTestRule.setContent {
            MarkdownRenderer(SAMPLE_FULL_MARKDOWN, ReaderPreferences())
        }
        
        composeTestRule.onRoot().captureRoboImage("screenshots/full_markdown_rendering.png")
    }
}
```

---

## 6. Fase 5: Manejo de Errores y Logging

### 6.1 Sistema de Error Handling (Sealed Class)

```kotlin
// domain/model/Result.kt
sealed class DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>()
    data class Error(val exception: Throwable, val userMessage: String) : DomainResult<Nothing>()
    object Loading : DomainResult<Nothing>()
}

// Uso en ViewModel
fun loadDocument(id: Int) {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        
        when (val result = getDocumentUseCase(id)) {
            is DomainResult.Success -> _uiState.value = UiState.Success(result.data)
            is DomainResult.Error -> _uiState.value = UiState.Error(result.userMessage)
            is DomainResult.Loading -> _uiState.value = UiState.Loading
        }
    }
}
```

### 6.2 Logging Estructurado (Timber para Android)

```kotlin
// data/local/TimberLogger.kt
class TimberLogger @Inject constructor() : AppLogger {
    override fun debug(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }
    
    override fun error(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).e(throwable, message)
        // En producción, reportar a Firebase Crashlytics
        if (throwable != null) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }
}
```

### 6.3 Manejo de Excepciones en UI

```kotlin
@Composable
fun ErrorHandler(error: Throwable?, onRetry: () -> Unit) {
    val message = when (error) {
        is DatabaseIOException -> "Could not access database. Please try again."
        is SecurityException -> "Permission denied."
        is NetworkException -> "Network error. Check your connection."
        else -> "An unexpected error occurred."
    }
    
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = { 
            Button(onClick = onRetry) { Text("Retry") } 
        }
    )
}
```

---

## 7. Fase 6: Optimización UI/UX

### 7.1 Animaciones y Transiciones

```kotlin
// Animaciones para cambios de modo (lectura <-> edición)
AnimatedContent(
    targetState = isEditMode,
    transitionSpec = {
        (fadeIn(animationSpec = tween(220, delayMillis = 90)) 
         with fadeOut(animationSpec = tween(90)))
            .using(SizeTransform(clip = false))
    }
) { mode ->
    if (mode) EditorScreen() else ReaderScreen()
}

// Animación para lista de documentos
LazyColumn {
    items(documents, key = { it.id }) { doc ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            DocumentListItem(doc = doc)
        }
    }
}
```

### 7.2 Soporte para Configuraciones Especiales

```kotlin
// Soporte para tamaños de fuente del sistema
val fontScale = LocalDensity.current.fontScale
val adjustedSize = (baseSize / fontScale).sp

// Soporte para TalkBack (accesibilidad)
Modifier.semantics {
    stateDescription = if (item.isChecked) "Completed" else "Pending"
    onClick(action = { toggleItem(); true }, label = "Toggle task")
}

// Soporte para modo oscuro automático
val isDarkTheme = isSystemInDarkTheme()
```

### 7.3 Responsive Design Mejorado

```kotlin
// Layout adaptativo basado en tamaño de ventana
@Composable
fun AppScaffold(windowSizeClass: WindowSizeClass) {
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> MobileLayout()
        WindowWidthSizeClass.Medium -> TabletLayout()
        WindowWidthSizeClass.Expanded -> DesktopLayout()
    }
}
```

---

## 8. Fase 7: Preparación para Release

### 8.1 Firma del APK/APK

```bash
# Generar keystore (si no existe)
keytool -genkey -v -keystore marked-keystore.jks \\
    -alias marked \\
    -keyalg RSA -keysize 2048 -validity 10000

# Configurar en build.gradle.kts
signingConfigs {
    create("release") {
        storeFile = file("marked-keystore.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "marked"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

### 8.2 Configuración de ProGuard Optimizada

```proguard
# proguard-rules.pro (completo)
# ... [ver sección 4.3] ...
```

### 8.3 Configuración de CI/CD (GitHub Actions)

```yaml
# .github/workflows/build.yml
name: Build & Test

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      - name: Run unit tests
        run: ./gradlew test
      
      - name: Run lint
        run: ./gradlew lint
      
      - name: Build APK
        run: ./gradlew assembleRelease
      
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: release-apk
          path: app/build/outputs/apk/release/*.apk
```

---

## 9. Cronograma de Implementación

| Fase | Duración Estimada | Prioridad |
|------|-------------------|-----------|
| 1. Refactorización Arquitectónica | 2-3 semanas | 🔴 Crítico |
| 2. Inyección de Dependencias (Hilt) | 1 semana | 🔴 Crítico |
| 3. Rendimiento y Estabilidad | 1-2 semanas | 🔴 Crítico |
| 4. Testing Exhaustivo | 2-3 semanas | 🟡 Alto |
| 5. Manejo de Errores y Logging | 1 semana | 🟡 Alto |
| 6. UI/UX y Accesibilidad | 1-2 semanas | 🟢 Medio |
| 7. Preparación para Release | 1 semana | 🟢 Medio |

**Total Estimado**: 9-13 semanas (2-3 meses con un desarrollador full-time)  
**Con equipo de 2-3 desarrolladores**: 5-7 semanas

---

## 10. Checklist de Validación

### Checklist de Arquitectura
- [ ] MainActivity tiene < 100 líneas
- [ ] Existe capa de dominio (Use Cases)
- [ ] Interfaces de repositorio en domain/
- [ ] Implementaciones de repositorio en data/
- [ ] MarkdownParser es pure Kotlin (sin Compose)
- [ ] Renderers separados por tipo de bloque

### Checklist de Calidad
- [ ] Cobertura de tests > 80%
- [ ] Tests de ViewModel existen
- [ ] Tests de Repositorio existen
- [ ] Tests de UI existen (Espresso/Compose UI Test)
- [ ] Tests de Screenshot (Roborazzi)
- [ ] ProGuard configurado para release
- [ ] CI/CD funcional (GitHub Actions)

### Checklist de Rendimiento
- [ ] Parseo de Markdown cacheado
- [ ] Debounce para auto-guardado implementado
- [ ] LazyColumn para documentos largos
- [ ] No hay memory leaks detectados
- [ ] Tiempo de inicio < 2 segundos

### Checklist de UX/UI
- [ ] Animaciones entre lectura/ edición
- [ ] Soporte para TalkBack
- [ ] Soporte para tamaños de fuente del sistema
- [ ] Modo oscuro automático funciona
- [ ] Responsive en tablet y foldables

### Checklist de Seguridad
- [ ] Sanitización de URLs en HTML exportado
- [ ] Escapado de HTML en títulos/contenido
- [ ] JavaScript deshabilitado en WebView
- [ ] FileProvider configurado correctamente
- [ ] Keystore protegido (no en repo)

---

> **Nota Final**: Este plan representa una transformación fundamental del código base actual. Se recomienda realizar la migración por fases, comenzando por la separación de responsabilidades y la inyección de dependencias, que son los cimientos sobre los que se construirá todo el resto.
