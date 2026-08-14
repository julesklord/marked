package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.animation.Crossfade
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.MarkdownDocument
import com.example.data.MarkdownRepository
import androidx.compose.ui.res.stringResource
import com.example.ui.markdown.*
import com.example.ui.viewmodel.MarkdownViewModel
import com.example.ui.viewmodel.MarkdownViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MarkdownRepository(database.markdownDao())
        val preferencesStore = ReaderPreferencesStore(applicationContext)
        val viewModel: MarkdownViewModel by viewModels { MarkdownViewModelFactory(repository, preferencesStore, applicationContext) }

        setContent {
            val systemInDark = isSystemInDarkTheme()

            // Pick a default theme from the system dark mode only on first launch; saved choices persist.
            LaunchedEffect(systemInDark) {
                viewModel.applyDefaultThemeIfFirstLaunch(systemInDark)
            }

            val preferences by viewModel.readerPreferences.collectAsStateWithLifecycle()
            val theme = preferences.selectedTheme
            val bgColor = Color(theme.hexBackground)

            // Dynamic layout scaffolding driven by selected reader themes
            MaterialTheme(
                colorScheme = if (theme.isDark) {
                    darkColorScheme(
                        primary = Color(theme.hexAccent),
                        background = bgColor,
                        surface = Color(theme.hexCodeBg)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(theme.hexAccent),
                        background = bgColor,
                        surface = Color(theme.hexCodeBg)
                    )
                }
            ) {
                val containerModifier = if (theme == ReaderTheme.IMMERSIVE_UI) {
                    Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F1113), Color(0xFF16181B))
                            )
                        )
                } else {
                    Modifier
                        .fillMaxSize()
                        .background(bgColor)
                }
                Box(modifier = containerModifier) {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MarkdownViewModel) {
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val selectedDoc by viewModel.selectedDocument.collectAsStateWithLifecycle()
    val preferences by viewModel.readerPreferences.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val context = LocalContext.current
        val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var isMobileSidebarOpen by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<MarkdownDocument?>(null) }
    var showDeleteDialog by remember { mutableStateOf<MarkdownDocument?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val theme = preferences.selectedTheme
    val bgColor = Color(theme.hexBackground)
    val fgColor = Color(theme.hexForeground)
    val accentColor = Color(theme.hexAccent)

    val listScrollState = rememberScrollState()

    // Handle back button on mobile: if sidebar is open, close it, else if in edit mode, toggle back to reading mode
    BackHandler(enabled = isMobileSidebarOpen || isEditMode) {
        if (isMobileSidebarOpen) {
            isMobileSidebarOpen = false
        } else {
            viewModel.setEditMode(false)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth > 680.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar Panel: Always visible on tablet, drawer-slider overlay on mobile
            if (isTablet) {
                SidebarContent(
                    documents = documents,
                    selectedDoc = selectedDoc,
                    searchQuery = searchQuery,
                    theme = theme,
                    preferences = preferences,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onSelectDoc = {
                        viewModel.selectDocument(it)
                        viewModel.setEditMode(false)
                    },
                    onCreateNew = { showCreateDialog = true },
                    onRename = { showRenameDialog = it },
                    onDelete = { showDeleteDialog = it },
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(if (theme.isDark) bgColor.copy(alpha = 0.95f) else Color(theme.hexCodeBg))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (theme.isDark) Color(theme.hexCodeBg) else Color(theme.hexQuoteBar).copy(alpha = 0.5f)
                            )
                        )
                )
            }

            // Main View Area: Render reading or editing workspaces
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Text(
                            text = selectedDoc?.title ?: stringResource(R.string.not_selected),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                fontFamily = preferences.selectedFont.fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = Color(theme.hexHeader)
                            )
                        )
                    },
                    navigationIcon = {
                        if (!isTablet) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text(stringResource(R.string.folder_notes)) } },
                                state = rememberTooltipState()
                            ) {
                                IconButton(
                                    onClick = { isMobileSidebarOpen = !isMobileSidebarOpen },
                                    modifier = Modifier.testTag("menu_sidebar_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = stringResource(R.string.folder_notes),
                                        tint = accentColor
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.padding(12.dp)) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null, // decorative in tablet view
                                    tint = accentColor
                                )
                            }
                        }
                    },
                    actions = {
                        // Edit / Read Toggle Switch
                        if (selectedDoc != null) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text(if (isEditMode) stringResource(R.string.read_mode) else stringResource(R.string.edit_mode)) } },
                                state = rememberTooltipState()
                            ) {
                                IconButton(
                                    onClick = { viewModel.setEditMode(!isEditMode) },
                                    modifier = Modifier.testTag("toggle_edit_mode_button")
                                ) {
                                    Icon(
                                        imageVector = if (isEditMode) Icons.Default.Book else Icons.Default.Edit,
                                        contentDescription = if (isEditMode) stringResource(R.string.read_mode) else stringResource(R.string.edit_mode),
                                        tint = accentColor
                                    )
                                }
                            }
                        }

                        // Reading config settings
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(stringResource(R.string.pref_dialog_title)) } },
                            state = rememberTooltipState()
                        ) {
                            IconButton(
                                onClick = { showSettingsSheet = true },
                                modifier = Modifier.testTag("configure_font_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFormat,
                                    contentDescription = stringResource(R.string.pref_dialog_title),
                                    tint = fgColor.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // More Action options
                        if (selectedDoc != null) {
                            var showDropdown by remember { mutableStateOf(false) }
                            Box {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = { PlainTooltip { Text(stringResource(R.string.options)) } },
                                    state = rememberTooltipState()
                                ) {
                                    IconButton(onClick = { showDropdown = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.options),
                                            tint = fgColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showDropdown,
                                    onDismissRequest = { showDropdown = false },
                                    modifier = Modifier.background(bgColor)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.rename_note), color = fgColor) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DriveFileRenameOutline,
                                                contentDescription = null,
                                                tint = accentColor
                                            )
                                        },
                                        onClick = {
                                            showDropdown = false
                                            showRenameDialog = selectedDoc
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.export_html), color = fgColor) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = null,
                                                tint = accentColor
                                            )
                                        },
                                        onClick = {
                                            showDropdown = false
                                            selectedDoc?.let { doc ->
                                                MarkdownExporter.exportHtml(context, doc, preferences)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.export_pdf), color = fgColor) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Print,
                                                contentDescription = null,
                                                tint = accentColor
                                            )
                                        },
                                        onClick = {
                                            showDropdown = false
                                            selectedDoc?.let { doc ->
                                                MarkdownExporter.printPdf(context, doc, preferences)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_note), color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showDropdown = false
                                            showDeleteDialog = selectedDoc
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (theme.isDark) bgColor else Color(theme.hexCodeBg).copy(alpha = 0.15f),
                        titleContentColor = Color(theme.hexHeader)
                    ),
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                )

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = if (theme.isDark) Color(theme.hexCodeBg) else Color(theme.hexQuoteBar).copy(alpha = 0.3f)
                )

                // Workspace content body
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Crossfade(
                        targetState = Pair(selectedDoc, isEditMode),
                        label = "Workspace Mode Transition"
                    ) { (currentDoc, currentIsEditMode) ->
                        if (currentDoc == null) {
                            // Empty launch screen view
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = stringResource(R.string.empty_notes_icon),
                                    tint = accentColor.copy(alpha = 0.3f),
                                    modifier = Modifier.size(96.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.no_documents_selected),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = fgColor.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.create_or_open_templates),
                                    fontSize = 14.sp,
                                    color = fgColor.copy(alpha = 0.5f),
                                    style = TextStyle(fontStyle = FontStyle.Italic)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { showCreateDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.new_document), color = Color(theme.hexBackground))
                                }
                            }
                        } else {
                            if (currentIsEditMode) {
                                // High Custom Markdown raw editor with fast editing shortcuts
                                MarkdownEditorArea(
                                    document = currentDoc,
                                    preferences = preferences,
                                    onContentChange = { viewModel.updateDocumentContent(it) }
                                )
                            } else {
                                // High Quality document reader
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    MarkdownRenderer(
                                        content = currentDoc.content,
                                        preferences = preferences,
                                        onToggleChecklist = { lineIndex ->
                                            viewModel.toggleChecklistItem(lineIndex)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(64.dp)) // Floating back-layer space
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mobile Sidebar Drawer Slider overlay
        if (!isTablet && isMobileSidebarOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { isMobileSidebarOpen = false }
            ) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(if (theme.isDark) bgColor else Color(theme.hexCodeBg))
                        .clickable(enabled = false) {}
                        .align(Alignment.CenterStart)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    SidebarContent(
                        documents = documents,
                        selectedDoc = selectedDoc,
                        searchQuery = searchQuery,
                        theme = theme,
                        preferences = preferences,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onSelectDoc = {
                            viewModel.selectDocument(it)
                            viewModel.setEditMode(false)
                            isMobileSidebarOpen = false
                        },
                        onCreateNew = {
                            showCreateDialog = true
                            isMobileSidebarOpen = false
                        },
                        onRename = { showRenameDialog = it },
                        onDelete = { showDeleteDialog = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Font Configuration Custom Controls Sheet (Tactile Dialog overlay to avoid sheet version bugs)
        if (showSettingsSheet) {
            AlertDialog(
                onDismissRequest = { showSettingsSheet = false },
                confirmButton = {
                    TextButton(onClick = { showSettingsSheet = false }) {
                        Text(stringResource(R.string.accept), color = accentColor, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text(
                        stringResource(R.string.pref_dialog_title),
                        style = TextStyle(
                            fontFamily = preferences.selectedFont.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(theme.hexHeader)
                        )
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Typography Selection Row
                        Text(
                            stringResource(R.string.pref_font_title),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = fgColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderFontFamily.entries.forEach { font ->
                                val selected = preferences.selectedFont == font
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) accentColor else Color(theme.hexCodeBg))
                                        .clickable {
                                            viewModel.updatePreferences { it.copy(selectedFont = font) }
                                        }
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (selected) accentColor else fgColor.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(font.displayNameResId),
                                        style = TextStyle(
                                            fontFamily = font.fontFamily,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = if (selected) Color(theme.hexBackground) else fgColor
                                        )
                                    )
                                }
                            }
                        }

                        // Font size control with live slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.pref_font_size, preferences.fontSizeSp.toInt()),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = fgColor.copy(alpha = 0.7f)
                            )
                        }
                        Slider(
                            value = preferences.fontSizeSp,
                            onValueChange = { size ->
                                viewModel.updatePreferences { it.copy(fontSizeSp = size) }
                            },
                            valueRange = 14f..28f,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = fgColor.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Line Spacing multipliers
                        Text(
                            stringResource(R.string.pref_spacing_title),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = fgColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Pair(stringResource(R.string.spacing_compact), 1.2f),
                                Pair(stringResource(R.string.spacing_standard), 1.5f),
                                Pair(stringResource(R.string.spacing_spacious), 1.8f)
                            ).forEach { pair ->
                                val selected = preferences.lineSpacingMultiplier == pair.second
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) accentColor else Color(theme.hexCodeBg))
                                        .clickable {
                                            viewModel.updatePreferences { it.copy(lineSpacingMultiplier = pair.second) }
                                        }
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (selected) accentColor else fgColor.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pair.first,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) Color(theme.hexBackground) else fgColor
                                        )
                                    )
                                }
                            }
                        }

                        // Theme Mode colors pallet
                        Text(
                            stringResource(R.string.pref_theme_title),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = fgColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderTheme.entries.forEach { rt ->
                                val selected = preferences.selectedTheme == rt
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.updatePreferences { it.copy(selectedTheme = rt) }
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(rt.hexBackground))
                                            .border(
                                                BorderStroke(
                                                    if (selected) 2.dp else 1.dp,
                                                    if (selected) accentColor else fgColor.copy(alpha = 0.2f)
                                                ),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Ab",
                                            color = Color(rt.hexForeground),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            fontFamily = preferences.selectedFont.fontFamily
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(rt.displayNameResId),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        color = if (selected) accentColor else fgColor.copy(alpha = 0.6f),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                },
                containerColor = bgColor,
                modifier = Modifier.testTag("reading_preferences_dialog")
            )
        }

        // Overlay dialog interfaces:
        // CREATE NOTE DIALOG
        if (showCreateDialog) {
            var inputTitle by remember { mutableStateOf("") }
            val isInputValid = inputTitle.isNotBlank()
            val focusRequester = remember { FocusRequester() }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text(stringResource(R.string.create_dialog_title), color = Color(theme.hexHeader)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.create_dialog_text),
                            fontSize = 13.sp,
                            color = fgColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            placeholder = { Text(stringResource(R.string.create_dialog_placeholder)) },
                            trailingIcon = {
                                if (inputTitle.isNotEmpty()) {
                                    IconButton(onClick = { inputTitle = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.clear_input),
                                            tint = fgColor.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = fgColor,
                                unfocusedTextColor = fgColor,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = fgColor.copy(alpha = 0.3f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (isInputValid) {
                                    viewModel.createNewDocument(inputTitle.trim())
                                    showCreateDialog = false
                                }
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("create_document_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isInputValid) {
                                viewModel.createNewDocument(inputTitle.trim())
                                showCreateDialog = false
                            }
                        },
                        enabled = isInputValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            disabledContainerColor = accentColor.copy(alpha = 0.5f),
                            disabledContentColor = Color(theme.hexBackground).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("confirm_create_button")
                    ) {
                        Text(stringResource(R.string.btn_create), color = Color(theme.hexBackground))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text(stringResource(R.string.btn_cancel), color = fgColor.copy(alpha = 0.6f))
                    }
                },
                containerColor = bgColor
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        // RENAME NOTE DIALOG
        if (showRenameDialog != null) {
            val docToRename = showRenameDialog!!
            var inputTitle by remember { mutableStateOf(docToRename.title) }
            val isInputValid = inputTitle.isNotBlank()
            val focusRequester = remember { FocusRequester() }
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                title = { Text(stringResource(R.string.rename_dialog_title), color = Color(theme.hexHeader)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            trailingIcon = {
                                if (inputTitle.isNotEmpty()) {
                                    IconButton(onClick = { inputTitle = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.clear_input),
                                            tint = fgColor.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = fgColor,
                                unfocusedTextColor = fgColor,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = fgColor.copy(alpha = 0.3f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (isInputValid) {
                                    viewModel.renameDocument(docToRename.id, inputTitle.trim())
                                    showRenameDialog = null
                                }
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("rename_document_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isInputValid) {
                                viewModel.renameDocument(docToRename.id, inputTitle.trim())
                                showRenameDialog = null
                            }
                        },
                        enabled = isInputValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            disabledContainerColor = accentColor.copy(alpha = 0.5f),
                            disabledContentColor = Color(theme.hexBackground).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("confirm_rename_button")
                    ) {
                        Text(stringResource(R.string.btn_save), color = Color(theme.hexBackground))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                },
                containerColor = bgColor
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        // CONFIRM DELETE DIALOG
        if (showDeleteDialog != null) {
            val docToDelete = showDeleteDialog!!
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text(stringResource(R.string.delete_dialog_title), color = MaterialTheme.colorScheme.error) },
                text = {
                    Text(stringResource(R.string.delete_dialog_text, docToDelete.title), color = fgColor)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteDocument(docToDelete)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text(stringResource(R.string.btn_delete), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text(stringResource(R.string.btn_back), color = fgColor.copy(alpha = 0.6f))
                    }
                },
                containerColor = bgColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarContent(
    documents: List<MarkdownDocument>,
    selectedDoc: MarkdownDocument?,
    searchQuery: String,
    theme: ReaderTheme,
    preferences: ReaderPreferences,
    onSearchChange: (String) -> Unit,
    onSelectDoc: (MarkdownDocument) -> Unit,
    onCreateNew: () -> Unit,
    onRename: (MarkdownDocument) -> Unit,
    onDelete: (MarkdownDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(theme.hexAccent)
    val textBgColor = Color(theme.hexCodeBg)
    val fgColor = Color(theme.hexForeground)
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Filter documents dynamically
    val filteredDocs = remember(documents, searchQuery) {
        if (searchQuery.isBlank()) {
            documents
        } else {
            documents.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.padding(12.dp)) {
        // App Title branding and creator credits
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "M",
                    color = Color(theme.hexBackground),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Marked Reader",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color(theme.hexHeader),
                    letterSpacing = 0.5.sp
                )
                Text(
                    stringResource(R.string.app_subtitle),
                    fontSize = 10.sp,
                    color = fgColor.copy(alpha = 0.5f)
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 12.sp, color = fgColor.copy(0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.search_icon_description), modifier = Modifier.size(16.dp), tint = fgColor.copy(0.4f)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(stringResource(R.string.clear_search)) } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = fgColor.copy(0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = fgColor,
                unfocusedTextColor = fgColor,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = fgColor.copy(0.15f),
                focusedContainerColor = textBgColor.copy(alpha = 0.5f),
                unfocusedContainerColor = textBgColor.copy(alpha = 0.5f)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("document_search_field")
        )

        // List Header actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.documents_header, filteredDocs.size),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = fgColor.copy(alpha = 0.4f),
                letterSpacing = 0.8.sp
            )
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(stringResource(R.string.new_document)) } },
                state = rememberTooltipState()
            ) {
                IconButton(
                    onClick = onCreateNew,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("create_document_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = stringResource(R.string.new_document),
                        tint = accentColor
                    )
                }
            }
        }

        // Document item List
        if (filteredDocs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.words_not_found),
                    fontSize = 12.sp,
                    color = fgColor.copy(alpha = 0.4f),
                    fontStyle = FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (searchQuery.isNotEmpty()) {
                    androidx.compose.material3.TextButton(onClick = { onSearchChange("") }) {
                        Text(stringResource(R.string.clear_search), color = accentColor)
                    }
                } else {
                    androidx.compose.material3.TextButton(onClick = onCreateNew) {
                        Text(stringResource(R.string.new_document), color = accentColor)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("document_list")
            ) {
                items(filteredDocs, key = { it.id }) { doc ->
                    val isSelected = selectedDoc?.id == doc.id
                    val wordCount = doc.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) {
                                    if (theme == ReaderTheme.IMMERSIVE_UI) Color(0xFF004786).copy(alpha = 0.8f) else accentColor.copy(alpha = 0.15f)
                                } else Color.Transparent
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isSelected) {
                                        if (theme == ReaderTheme.IMMERSIVE_UI) Color(0xFFD1E4FF).copy(alpha = 0.3f) else accentColor.copy(alpha = 0.3f)
                                    } else Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectDoc(doc) }
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .testTag("document_item_${doc.id}")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = doc.title,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isSelected) {
                                        if (theme == ReaderTheme.IMMERSIVE_UI) Color(0xFFD1E4FF) else accentColor
                                    } else Color(theme.hexHeader),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // Item secondary commands in sidebar
                                Row {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                        tooltip = { PlainTooltip { Text(stringResource(R.string.rename_note)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(
                                            onClick = { onRename(doc) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DriveFileRenameOutline,
                                                contentDescription = stringResource(R.string.rename_note),
                                                tint = fgColor.copy(alpha = 0.3f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                        tooltip = { PlainTooltip { Text(stringResource(R.string.delete_note)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(
                                            onClick = { onDelete(doc) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.delete_note),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.word_count, wordCount),
                                    fontSize = 11.sp,
                                    color = if (isSelected && theme == ReaderTheme.IMMERSIVE_UI) Color(0xFFD1E4FF).copy(alpha = 0.7f) else fgColor.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = formatRelativeTime(context, doc.updatedAt),
                                    fontSize = 10.sp,
                                    color = if (isSelected && theme == ReaderTheme.IMMERSIVE_UI) Color(0xFF004786).copy(alpha = 0.0f).run { Color(0xFFD1E4FF).copy(alpha = 0.7f) } else fgColor.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownEditorArea(
    document: MarkdownDocument,
    preferences: ReaderPreferences,
    onContentChange: (String) -> Unit
) {
    val theme = preferences.selectedTheme
    val bgColor = Color(theme.hexBackground)
    val textBgColor = Color(theme.hexCodeBg)
    val fgColor = Color(theme.hexForeground)
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val accentColor = Color(theme.hexAccent)

    // Using a stateful TextFieldValue to preserve cursor position when formatting
    var textFieldValueState by remember(document.id) {
        mutableStateOf(TextFieldValue(text = document.content, selection = TextRange(document.content.length)))
    }

    // Sync state text with DB updates from checklist or other updates safely
    LaunchedEffect(document.content) {
        if (textFieldValueState.text != document.content) {
            textFieldValueState = textFieldValueState.copy(text = document.content)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Raw Markdown input editor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            BasicTextField(
                value = textFieldValueState,
                onValueChange = { newValue ->
                    textFieldValueState = newValue
                    onContentChange(newValue.text)
                },
                textStyle = TextStyle(
                    fontFamily = preferences.selectedFont.fontFamily,
                    fontSize = preferences.fontSizeSp.sp,
                    color = fgColor,
                    lineHeight = (preferences.fontSizeSp * 1.35f).sp
                ),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = true,
                    imeAction = ImeAction.Default
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("markdown_text_field"),
                decorationBox = { innerTextField ->
                    if (textFieldValueState.text.isEmpty()) {
                        Text(
                            stringResource(R.string.editor_placeholder),
                            style = TextStyle(
                                fontFamily = preferences.selectedFont.fontFamily,
                                fontSize = preferences.fontSizeSp.sp,
                                color = fgColor.copy(alpha = 0.35f),
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Horizontal Formatting Quick Helpers Bar centered above virtual keyboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(textBgColor)
                .border(BorderStroke(1.dp, fgColor.copy(alpha = 0.1f)))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val insertFormatSymbol = { prefix: String, suffix: String ->
                val text = textFieldValueState.text
                val selection = textFieldValueState.selection
                
                // Secure bounds and direction-agnostic calculations
                val start = minOf(selection.start, selection.end).coerceIn(0, text.length)
                val end = maxOf(selection.start, selection.end).coerceIn(0, text.length)
                val selectedText = text.substring(start, end)
                
                // Smart newline handling for line-level formats (like lists, headers, blockquotes, horizontal rules)
                val isLineLevel = prefix.startsWith("\n") || suffix.startsWith("\n")
                val needsStartNewline = isLineLevel && start > 0 && text[start - 1] != '\n'
                val needsEndNewline = isLineLevel && end < text.length && text[end] != '\n'
                
                val cleanPrefix = if (isLineLevel) {
                    val p = prefix.trimStart('\n')
                    if (needsStartNewline) "\n$p" else p
                } else {
                    prefix
                }
                
                val cleanSuffix = if (isLineLevel) {
                    val s = suffix.trimEnd('\n')
                    val padded = if (needsEndNewline) "$s\n" else s
                    padded
                } else {
                    suffix
                }
                
                val replacement = "$cleanPrefix$selectedText$cleanSuffix"
                val newText = text.replaceRange(start, end, replacement)
                
                val newSelectionRange = if (selectedText.isEmpty()) {
                    TextRange((start + cleanPrefix.length).coerceIn(0, newText.length))
                } else {
                    TextRange(
                        start = (start + cleanPrefix.length).coerceIn(0, newText.length),
                        end = (start + cleanPrefix.length + selectedText.length).coerceIn(0, newText.length)
                    )
                }

                textFieldValueState = TextFieldValue(text = newText, selection = newSelectionRange)
                onContentChange(newText)
            }

            // Quick Format Buttons
            FormatToolbarButton(
                icon = Icons.Default.FormatBold,
                label = stringResource(R.string.tb_bold),
                theme = theme,
                onClick = { insertFormatSymbol("**", "**") }
            )
            FormatToolbarButton(
                icon = Icons.Default.FormatItalic,
                label = stringResource(R.string.tb_italic),
                theme = theme,
                onClick = { insertFormatSymbol("*", "*") }
            )
            FormatToolbarButton(
                icon = Icons.Default.Title,
                label = stringResource(R.string.tb_title),
                theme = theme,
                onClick = { insertFormatSymbol("\n# ", "\n") }
            )
            FormatToolbarButton(
                icon = Icons.Default.Code,
                label = stringResource(R.string.tb_code),
                theme = theme,
                onClick = { insertFormatSymbol("\n```kotlin\n", "\n```\n") }
            )
            FormatToolbarButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                label = stringResource(R.string.tb_item),
                theme = theme,
                onClick = { insertFormatSymbol("\n- ", "\n") }
            )
            FormatToolbarButton(
                icon = Icons.AutoMirrored.Filled.FactCheck,
                label = stringResource(R.string.tb_task),
                theme = theme,
                onClick = { insertFormatSymbol("\n- [ ] ", "\n") }
            )
            FormatToolbarButton(
                icon = Icons.Default.Link,
                label = stringResource(R.string.tb_link),
                theme = theme,
                onClick = { insertFormatSymbol("[", "](https://)") }
            )
            FormatToolbarButton(
                icon = Icons.Default.FormatQuote,
                label = stringResource(R.string.tb_quote),
                theme = theme,
                onClick = { insertFormatSymbol("\n> ", "\n") }
            )
            FormatToolbarButton(
                icon = Icons.Default.HorizontalRule,
                label = stringResource(R.string.tb_line),
                theme = theme,
                onClick = { insertFormatSymbol("\n---\n", "") }
            )
        }
    }
}

@Composable
fun FormatToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    theme: ReaderTheme,
    onClick: () -> Unit
) {
    val accentColor = Color(theme.hexAccent)
    val fgColor = Color(theme.hexForeground)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(theme.hexBackground),
            contentColor = fgColor
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.15f)),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // decorative in button with label text
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fgColor.copy(0.7f)
        )
    }
}

private fun formatRelativeTime(context: Context, timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return context.getString(R.string.time_now)
    val sec = diff / 1000
    if (sec < 60) return context.getString(R.string.time_moment)
    val min = sec / 60
    if (min < 60) return context.getString(R.string.time_min, min.toInt())
    val hr = min / 60
    if (hr < 24) return context.getString(R.string.time_hr, hr.toInt())
    val days = hr / 24
    if (days < 7) return context.getString(R.string.time_days, days.toInt())
    return java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(timestamp)
}
