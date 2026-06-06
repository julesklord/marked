# Marked 📑

**Marked** is a high-performance, lightweight Markdown reader and editor application designed exclusively for Android. Built with modern Android development practices (Kotlin, Jetpack Compose, Room Database, Preferences DataStore, and Material 3), it delivers a premium, highly aesthetic reading and editing experience for your notes.

---

## Key Features

- ⚡ **Premium Markdown Rendering**: 
  - Dynamic typography scale supporting Sans-Serif, Serif, and Monospace families.
  - **macOS Window Decorated Code Blocks**: Renders high-fidelity dark-themed code boxes featuring OS window control dots, copy-to-clipboard actions, and a comprehensive **One Dark** syntax highlighter for various languages (Kotlin, JS, numbers, strings, annotations, and comments).
  - **Editorial Blockquotes**: Features a modern glassmorphic background gradient with a low-opacity large quote symbol watermark.
  - **Interactive Checklists**: Double-tap directly on task checkboxes within the reader view to dynamically toggle check states with smooth ripple animations.
  - **Dashed Gradient Dividers**: Features elegant, fading horizontal rules with a central rotated diamond shape.
- 🎨 **Persistent Reader Settings**: Customize your reading layout (theme presets like Sepia Cozy, Pitch Black, or Immersive, font sizes, and line-spacing multipliers) persisted seamlessly across restarts using **Jetpack Preferences DataStore**.
- 📂 **Local Storage**: Auto-saves your notes locally in a structured SQLite database using **Room**.
- 📤 **Document Export**: 
  - Generates theme-matching, print-ready HTML files with built-in URL sanitization to prevent injection vectors.
  - Supports instant PDF rendering and printing via the Android `PrintManager`.
- 🔒 **Security Hardening**: Includes scoped `FileProvider` path restrictions and customized cloud/device data transfer exclusion policies.

---

## Project Structure & Clean Architecture

The project has been fully refactored to separate visual compositions from business logic:

```
app/src/main/java/com/example/
│
├── MainActivity.kt                  # App entry point, coordinates edge-to-edge Scaffold & top bar
│
├── data/                            # Room database persistence layer
│   ├── AppDatabase.kt
│   ├── MarkdownDao.kt
│   ├── MarkdownDocument.kt          # Entity modeling note data & metadata
│   └── MarkdownRepository.kt
│
├── ui/
│   ├── components/                  # Modular, decoupled visual components
│   │   ├── SidebarContent.kt        # Folder structure navigation and search drawer
│   │   ├── MarkdownEditorArea.kt    # Input area with formatting helper toolbar
│   │   ├── ReadingPreferencesDialog.kt # Layout configuration menu
│   │   └── DocumentDialogs.kt       # Create, Rename, and Delete note overlays
│   │
│   ├── markdown/                    # Markdown processing engine
│   │   ├── MarkdownParser.kt        # Custom line-by-line markdown AST parser
│   │   ├── MarkdownRenderer.kt      # Renders abstract blocks into Compose UI components
│   │   ├── MarkdownExporter.kt      # Compiles documents to styled HTML / WebView PDF
│   │   ├── ReaderPreferencesStore.kt # DataStore preference serialization
│   │   └── ReaderPreferences.kt
│   │
│   └── theme/                       # Material 3 typography and palette tokens
```

---

## Local Setup & Run

### Prerequisites
- [Android Studio Ladybug (or newer)](https://developer.android.com/studio)
- JDK 17 or 21 (Gradle build runs automatically with Java 21)
- Android SDK 36

### Getting Started

1. **Clone the repository** and open the project in Android Studio.
2. **Setup environment variables**:
   Create a `.env` file in the project's root folder and specify your Gemini API key (though the core app works offline):
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
3. **Automatic Keystore Decoding**:
   The build script is pre-configured to automatically decode the versioned `debug.keystore.base64` signing asset into `debug.keystore` during the Gradle configuration phase. No manual keystore generation or build.gradle modifications are required.
4. **Run**:
   Connect an Android Virtual Device (AVD) or a physical developer device via ADB, and select **Run app**.
