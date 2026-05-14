package com.kepat.unscopemydata.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.kepat.unscopemydata.data.ManifestManager
import com.kepat.unscopemydata.data.ShizukuFileManager
import com.kepat.unscopemydata.data.SyncFolder

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import android.content.Intent
import android.net.Uri
import android.os.Environment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var manifest by remember { mutableStateOf(ManifestManager.loadManifest()) }
    var lastModified by remember { mutableLongStateOf(ManifestManager.getManifestLastModified()) }
    var showBrowser by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    // Periodically poll for manifest changes and Shizuku connectivity (every 5 seconds)
    LaunchedEffect(Unit) {
        while (true) {
            // Only poll if we aren't currently performing a heavy sync operation
            if (!isSyncing && ShizukuFileManager.isBound) {
                try {
                    val currentModified = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        ManifestManager.getManifestLastModified()
                    }
                    
                    if (currentModified != lastModified) {
                        val updatedManifest = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            ManifestManager.loadManifest()
                        }
                        manifest = updatedManifest
                        lastModified = currentModified
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainScreen", "Polling error", e)
                }
            }

            // Also check Shizuku status and attempt to bind if disconnected
            if (!ShizukuFileManager.isBound) {
                ShizukuFileManager.bindService()
            }
            
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UnscopeMyData") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!isSyncing) showBrowser = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add, 
                    contentDescription = "Add Folder",
                    modifier = Modifier.alpha(if (isSyncing) 0.38f else 1.0f)
                )
            }
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            isSyncing = true
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val deviceName = Build.MODEL
                                val currentDate = dateFormat.format(Date())
                                var allSuccess = true
                                manifest.folders.forEach { 
                                    if (!ShizukuFileManager.pushData(it)) allSuccess = false
                                }
                                
                                val updated = manifest.copy(folders = manifest.folders.map { 
                                    it.copy(mostRecentUpdator = deviceName, dateMovedFromDataToUnscoped = currentDate) 
                                }.toMutableList())
                                ManifestManager.saveManifest(updated)
                                
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    manifest = updated
                                    isSyncing = false
                                    Toast.makeText(context, if (allSuccess) "Sync to Unscoped Successful" else "Sync to Unscoped Failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSyncing && ShizukuFileManager.isBound
                    ) {
                        if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("data->unscoped")
                    }
                    Button(
                        onClick = {
                            isSyncing = true
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                var allSuccess = true
                                manifest.folders.forEach { 
                                    if (!ShizukuFileManager.pullData(it)) allSuccess = false
                                }
                                
                                val updated = manifest.copy(folders = manifest.folders.toMutableList())
                                ManifestManager.saveManifest(updated)
                                
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    manifest = updated
                                    isSyncing = false
                                    Toast.makeText(context, if (allSuccess) "Sync to Data Successful" else "Sync to Data Failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSyncing && ShizukuFileManager.isBound
                    ) {
                        if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("unscoped->data")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(manifest.folders) { folder ->
                FolderCard(
                    folder = folder,
                    onDelete = {
                        val deletedFiles = ShizukuFileManager.deleteUnscopedData(folder)
                        if (deletedFiles) {
                            val updated = manifest.copy(folders = manifest.folders.filter { it.dataFolderName != folder.dataFolderName }.toMutableList())
                            ManifestManager.saveManifest(updated)
                            manifest = updated
                            Toast.makeText(context, "Config and Unscoped data deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to delete Unscoped data. Config preserved.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
        
        if (showBrowser) {
            FolderBrowserDialog(
                onDismiss = { showBrowser = false },
                onFolderSelected = { path, name ->
                    val newFolder = SyncFolder(name, path, Build.MODEL, "Never")
                    val updated = manifest.copy(folders = (manifest.folders + newFolder).toMutableList())
                    ManifestManager.saveManifest(updated)
                    manifest = updated
                    showBrowser = false
                }
            )
        }

        if (showSettings) {
            SettingsDialog(
                onDismiss = { showSettings = false },
                onSettingsSaved = {
                    manifest = ManifestManager.loadManifest()
                    showSettings = false
                }
            )
        }
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit, onSettingsSaved: () -> Unit) {
    var basePathInput by remember { mutableStateOf(com.kepat.unscopemydata.data.SettingsManager.basePath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                OutlinedTextField(
                    value = basePathInput,
                    onValueChange = { basePathInput = it },
                    label = { Text("Base Sync Path") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Relative to /storage/emulated/0/",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                
                Divider()
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Shizuku Status:", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = ShizukuFileManager.connectionStatus,
                        color = if (ShizukuFileManager.isBound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                com.kepat.unscopemydata.data.SettingsManager.basePath = basePathInput
                onSettingsSaved()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FolderCard(folder: SyncFolder, onDelete: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Name: ${folder.dataFolderName}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Path: ${folder.path}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Last Updator: ${folder.mostRecentUpdator}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Last Moved to Unscoped: ${folder.dateMovedFromDataToUnscoped}", style = MaterialTheme.typography.bodyMedium)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = {
                    try {
                        val base = com.kepat.unscopemydata.data.SettingsManager.basePath
                        val path = "${Environment.getExternalStorageDirectory().absolutePath}/$base/${folder.dataFolderName}"
                        val uri = Uri.parse(path)
                        
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(uri, "resource/folder") // Cx File Explorer specifically handles this
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        
                        // Fallback to generic directory type if the specific one fails
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            intent.setDataAndType(uri, "vnd.android.document/directory")
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open folder. Please ensure Cx File Explorer is installed.", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Icon(Icons.Default.Folder, contentDescription = "Open Unscoped Folder")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
