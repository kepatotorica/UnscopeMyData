package com.kepat.unscopemydata

import com.kepat.unscopemydata.IUserService
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import kotlin.system.exitProcess

class MyUserService : IUserService.Stub() {
    private val TAG = "MyUserService"

    init {
        Log.d(TAG, "MyUserService instantiated")
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun executeCommand(command: String): Boolean {
        Log.d(TAG, "Executing command: $command")
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitCode = process.waitFor()
            Log.d(TAG, "Command exit code: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed", e)
            false
        }
    }

    override fun listFiles(path: String): List<String> {
        val items = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "ls -1 \"$path\""))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { if (it.isNotBlank()) items.add(it) }
            }
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Listing files failed for $path", e)
        }
        return items
    }

    override fun isDirectory(path: String): Boolean {
        return try {
            val file = File(path)
            file.exists() && file.isDirectory
        } catch (e: Exception) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "[ -d \"$path\" ]"))
                process.waitFor() == 0
            } catch (e2: Exception) {
                false
            }
        }
    }

    override fun deleteDirectory(path: String): Boolean {
        Log.d(TAG, "Request to delete directory: $path")
        return try {
            val file = File(path)
            if (!file.exists()) {
                Log.d(TAG, "Path does not exist, nothing to delete: $path")
                return true
            }

            Log.d(TAG, "Executing: rm -rf \"$path\"")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "rm -rf \"$path\" 2>&1"))
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLines().joinToString("\n")
            
            val exitCode = process.waitFor()
            Log.d(TAG, "rm -rf exit code: $exitCode")
            if (output.isNotEmpty()) {
                Log.d(TAG, "rm -rf output: $output")
            }
            
            if (exitCode != 0) {
                Log.e(TAG, "Shell delete failed for $path with exit code $exitCode and output: $output")
                // Fallback to Java API if shell failed
                val success = file.deleteRecursively()
                Log.d(TAG, "Java fallback delete result for $path: $success")
            }

            // Final verification
            val stillExists = file.exists()
            if (stillExists) {
                Log.e(TAG, "Verification failed: Folder still exists after deletion attempt: $path")
                false
            } else {
                Log.d(TAG, "Successfully deleted: $path")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical failure during deletion of $path", e)
            false
        }
    }
}
