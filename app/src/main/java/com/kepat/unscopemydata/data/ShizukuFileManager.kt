package com.kepat.unscopemydata.data

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Environment
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kepat.unscopemydata.IUserService
import com.kepat.unscopemydata.MyUserService
import rikka.shizuku.Shizuku

import android.util.Log

object ShizukuFileManager {
    private const val TAG = "ShizukuFileManager"
    var isBound by mutableStateOf(false)
        private set

    var connectionStatus by mutableStateOf("Disconnected")
        private set

    private var userService: IUserService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "Service connected")
            userService = IUserService.Stub.asInterface(binder)
            isBound = true
            connectionStatus = "Connected"
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            userService = null
            isBound = false
            connectionStatus = "Disconnected"
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            val available = Shizuku.pingBinder()
            Log.d(TAG, "Shizuku available: $available")
            available
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Shizuku availability", e)
            false
        }
    }

    fun bindService() {
        if (userService != null) {
            Log.d(TAG, "Service already bound, skipping bind.")
            return
        }

        connectionStatus = "Binding..."
        if (isShizukuAvailable()) {
            try {
                Log.d(TAG, "Attempting to bind user service...")
                // We use the class name directly to ensure it matches the AIDL and manifest expectations
                val componentName = ComponentName("com.kepat.unscopemydata", MyUserService::class.java.name)
                val userServiceArgs = Shizuku.UserServiceArgs(componentName)
                    .daemon(false)
                    .debuggable(true)
                    .processNameSuffix("file_service")
                
                Shizuku.bindUserService(userServiceArgs, connection)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind user service", e)
                connectionStatus = "Error: ${e.message}"
            }
        } else {
            connectionStatus = "Shizuku Not Available (Ping failed)"
        }
    }

    fun unbindService() {
        if (userService != null) {
            try {
                val componentName = ComponentName("com.kepat.unscopemydata", MyUserService::class.java.name)
                val userServiceArgs = Shizuku.UserServiceArgs(componentName)
                    .daemon(false)
                    .processNameSuffix("file_service")
                Shizuku.unbindUserService(userServiceArgs, connection, true)
                userService = null
                isBound = false
                connectionStatus = "Unbound"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unbind user service", e)
            }
        }
    }

    fun executeCommand(command: String): Boolean {
        Log.d(TAG, "Requesting command execution: $command")
        return try {
            val result = userService?.executeCommand(command) ?: false
            Log.d(TAG, "Execution result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Remote error executing command", e)
            false
        }
    }

    fun getSyncDirPath(folderName: String): String {
        return "${Environment.getExternalStorageDirectory().absolutePath}/${SettingsManager.basePath}/$folderName"
    }

    fun pullData(syncFolder: SyncFolder): Boolean {
        val syncDirPath = getSyncDirPath(syncFolder.dataFolderName)
        val targetPath = syncFolder.path
        Log.d(TAG, "Pulling data from $syncDirPath to $targetPath")
        
        executeCommand("mkdir -p \"$syncDirPath\"")
        executeCommand("mkdir -p \"$targetPath\"")
        
        val command = "cp -rf \"$syncDirPath/.\" \"$targetPath/\""
        return executeCommand(command)
    }

    fun pushData(syncFolder: SyncFolder): Boolean {
        val syncDirPath = getSyncDirPath(syncFolder.dataFolderName)
        val targetPath = syncFolder.path
        Log.d(TAG, "Pushing data from $targetPath to $syncDirPath")
        
        executeCommand("mkdir -p \"$syncDirPath\"")
        
        val command = "cp -rf \"$targetPath/.\" \"$syncDirPath/\""
        return executeCommand(command)
    }
    
    fun listFoldersAndFiles(path: String): List<String> {
        Log.d(TAG, "Listing files for: $path")
        return try {
            val items = userService?.listFiles(path) ?: emptyList()
            Log.d(TAG, "Found ${items.size} items")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Remote error listing files", e)
            emptyList()
        }
    }

    fun isDirectory(path: String): Boolean {
        return try {
            userService?.isDirectory(path) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun deleteUnscopedData(syncFolder: SyncFolder): Boolean {
        val syncDirPath = getSyncDirPath(syncFolder.dataFolderName)
        Log.d(TAG, "Requesting deletion of: '$syncDirPath'")
        
        // Try local deletion first since SettingsManager.basePath is in public storage
        try {
            val file = java.io.File(syncDirPath)
            if (file.exists()) {
                Log.d(TAG, "Attempting local deletion for public storage path...")
                val localResult = file.deleteRecursively()
                if (localResult) {
                    Log.d(TAG, "Local deletion successful.")
                    return true
                }
                Log.w(TAG, "Local deletion failed, falling back to Shizuku.")
            } else {
                Log.d(TAG, "Path does not exist locally: $syncDirPath")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Local deletion error, trying Shizuku...", e)
        }

        return try {
            val currentService = userService
            if (currentService == null) {
                Log.w(TAG, "Cannot delete via Shizuku: userService is null. isBound: $isBound, status: $connectionStatus")
                return false
            }
            
            Log.d(TAG, "Calling Shizuku userService.deleteDirectory...")
            val result = currentService.deleteDirectory(syncDirPath)
            Log.d(TAG, "Shizuku deletion result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Remote error during deletion of '$syncDirPath'", e)
            false
        }
    }
}
