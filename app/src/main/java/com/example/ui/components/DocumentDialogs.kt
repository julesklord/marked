package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.MarkdownDocument
import com.example.ui.markdown.ReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDocumentDialog(
    theme: ReaderTheme,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val bgColor = Color(theme.hexBackground)
    val fgColor = Color(theme.hexForeground)
    val accentColor = Color(theme.hexAccent)
    var inputTitle by remember { mutableStateOf("") }
    val isInputValid = inputTitle.isNotBlank()
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
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
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                        tooltip = { PlainTooltip { Text(stringResource(R.string.clear_input)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = { inputTitle = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = stringResource(R.string.clear_input),
                                                tint = fgColor.copy(alpha = 0.6f)
                                            )
                                        }
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
                            onConfirm(inputTitle.trim())
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
                    if (isInputValid) onConfirm(inputTitle.trim())
                },
                enabled = isInputValid,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, disabledContainerColor = accentColor.copy(alpha = 0.5f), disabledContentColor = Color(theme.hexBackground).copy(alpha = 0.5f)),
                modifier = Modifier.testTag("confirm_create_button")
            ) {
                Text(stringResource(R.string.btn_create), color = Color(theme.hexBackground))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.btn_cancel), color = fgColor.copy(alpha = 0.6f))
            }
        },
        containerColor = bgColor
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameDocumentDialog(
    document: MarkdownDocument,
    theme: ReaderTheme,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val bgColor = Color(theme.hexBackground)
    val fgColor = Color(theme.hexForeground)
    val accentColor = Color(theme.hexAccent)
    var inputTitle by remember { mutableStateOf(document.title) }
    val isInputValid = inputTitle.isNotBlank()
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.rename_dialog_title), color = Color(theme.hexHeader)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inputTitle,
                    onValueChange = { inputTitle = it },
                    trailingIcon = {
                        if (inputTitle.isNotEmpty()) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                        tooltip = { PlainTooltip { Text(stringResource(R.string.clear_input)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = { inputTitle = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = stringResource(R.string.clear_input),
                                                tint = fgColor.copy(alpha = 0.6f)
                                            )
                                        }
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
                            onConfirm(inputTitle.trim())
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
                    if (isInputValid) onConfirm(inputTitle.trim())
                },
                enabled = isInputValid,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, disabledContainerColor = accentColor.copy(alpha = 0.5f), disabledContentColor = Color(theme.hexBackground).copy(alpha = 0.5f)),
                modifier = Modifier.testTag("confirm_rename_button")
            ) {
                Text(stringResource(R.string.btn_save), color = Color(theme.hexBackground))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.btn_cancel), color = fgColor.copy(alpha = 0.6f))
            }
        },
        containerColor = bgColor
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun DeleteDocumentDialog(
    document: MarkdownDocument,
    theme: ReaderTheme,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    val bgColor = Color(theme.hexBackground)
    val fgColor = Color(theme.hexForeground)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.delete_dialog_title), color = MaterialTheme.colorScheme.error) },
        text = {
            Text(
                stringResource(R.string.delete_dialog_text, document.title),
                color = fgColor
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(stringResource(R.string.btn_delete), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.btn_back), color = fgColor.copy(alpha = 0.6f))
            }
        },
        containerColor = bgColor
    )
}
