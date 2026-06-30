package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarkdownDocument
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.markdown.ReaderPreferences
import com.example.ui.markdown.ReaderTheme

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
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp), tint = fgColor.copy(0.4f)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.size(24.dp).testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_search),
                            tint = fgColor.copy(0.4f),
                            modifier = Modifier.size(16.dp)
                        )
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

        // Document item List
        if (filteredDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.words_not_found),
                    fontSize = 12.sp,
                    color = fgColor.copy(alpha = 0.4f),
                    fontStyle = FontStyle.Italic
                )
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
                    )
                }
            }
        }
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
