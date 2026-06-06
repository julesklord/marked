package com.example.ui.markdown

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_preferences")

/**
 * Persists [ReaderPreferences] (theme, font, sizes) across app restarts using Preferences DataStore.
 */
class ReaderPreferencesStore(private val context: Context) {

    private object Keys {
        val FONT = stringPreferencesKey("selected_font")
        val FONT_SIZE = floatPreferencesKey("font_size_sp")
        val LINE_SPACING = floatPreferencesKey("line_spacing_multiplier")
        val THEME = stringPreferencesKey("selected_theme")
        val INITIALIZED = booleanPreferencesKey("initialized")
    }

    val preferencesFlow: Flow<ReaderPreferences> = context.readerDataStore.data.map { prefs ->
        ReaderPreferences(
            selectedFont = prefs[Keys.FONT].toFontFamily(),
            fontSizeSp = prefs[Keys.FONT_SIZE] ?: ReaderPreferences().fontSizeSp,
            lineSpacingMultiplier = prefs[Keys.LINE_SPACING] ?: ReaderPreferences().lineSpacingMultiplier,
            selectedTheme = prefs[Keys.THEME].toTheme()
        )
    }

    /** True once the user (or first-launch defaulting) has written preferences at least once. */
    val isInitializedFlow: Flow<Boolean> = context.readerDataStore.data.map { prefs ->
        prefs[Keys.INITIALIZED] ?: false
    }

    suspend fun save(preferences: ReaderPreferences) {
        context.readerDataStore.edit { prefs ->
            prefs[Keys.FONT] = preferences.selectedFont.name
            prefs[Keys.FONT_SIZE] = preferences.fontSizeSp
            prefs[Keys.LINE_SPACING] = preferences.lineSpacingMultiplier
            prefs[Keys.THEME] = preferences.selectedTheme.name
            prefs[Keys.INITIALIZED] = true
        }
    }

    private fun String?.toFontFamily(): ReaderFontFamily =
        this?.let { name -> ReaderFontFamily.entries.find { it.name == name } } ?: ReaderPreferences().selectedFont

    private fun String?.toTheme(): ReaderTheme =
        this?.let { name -> ReaderTheme.entries.find { it.name == name } } ?: ReaderPreferences().selectedTheme
}
