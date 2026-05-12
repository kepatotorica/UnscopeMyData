package com.kepat.unscopemydata.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kepat.unscopemydata.data.ShizukuFileManager

import android.os.Environment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserDialog(
    onDismiss: () -> Unit,
    onFolderSelected: (String, String) -> Unit
) {
    val basePath = Environment.getExternalStorageDirectory().absolutePath
    var currentPath by remember { mutableStateOf("$basePath/Android/data") }
    var folderNameInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(emptyList<String>()) }
    var isLoading by remember { mutableStateOf(true) }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items else items.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(currentPath, ShizukuFileManager.isBound) {
        if (ShizukuFileManager.isBound) {
            isLoading = true
            items = ShizukuFileManager.listFoldersAndFiles(currentPath)
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxHeight(0.9f).fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Browse Folders", style = MaterialTheme.typography.titleLarge)
                Text(currentPath, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                
                if (!ShizukuFileManager.isBound) {
                    Text("Status: ${ShizukuFileManager.connectionStatus}", color = MaterialTheme.colorScheme.error)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search folders/files") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = {
                        val parent = currentPath.substringBeforeLast("/")
                        if (parent.length >= basePath.length) {
                            currentPath = parent
                            searchQuery = ""
                        }
                    }) {
                        Text("Up")
                    }
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading && ShizukuFileManager.isBound) {
                        CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                    } else if (items.isEmpty() && ShizukuFileManager.isBound) {
                        Text("No items found or access denied.", modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredItems) { item ->
                                ListItem(
                                    headlineContent = { Text(item) },
                                    modifier = Modifier.clickable {
                                        val newPath = if (currentPath.endsWith("/")) "$currentPath$item" else "$currentPath/$item"
                                        if (ShizukuFileManager.isDirectory(newPath)) {
                                            currentPath = newPath
                                            searchQuery = ""
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text("Data Folder Name (Unique)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onFolderSelected(currentPath, folderNameInput) },
                        enabled = folderNameInput.isNotBlank()
                    ) {
                        Text("Add Folder")
                    }
                }
            }
        }
    }
}
