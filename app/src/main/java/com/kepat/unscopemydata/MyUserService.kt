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
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "$command 2>&1"))
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLines().joinToString("\n")
            
            val exitCode = process.waitFor()
            Log.d(TAG, "Command exit code: $exitCode")
            if (output.isNotEmpty()) {
                Log.d(TAG, "Command output: $output")
            }
            
            if (exitCode != 0) {
                Log.e(TAG, "Command failed: $command | Exit: $exitCode | Output: $output")
            }
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during command execution: $command", e)
            false
        }
    }

    override fun listFiles(path: String): List<String> {
        val items = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "ls -1 \"$path\" 2>&1"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { if (it.isNotBlank()) items.add(it) }
            }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                Log.e(TAG, "ls -1 failed for $path with exit code $exitCode. Output: ${items.joinToString("\n")}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Listing files failed for $path", e)
        }
        return items
    }

    override fun isDirectory(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.isDirectory
            } else {
                // Fallback to shell test if Java API fails or reports non-existent
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "[ -d \"$path\" ]"))
                process.waitFor() == 0
            }
        } catch (e: Exception) {
            false
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

    override fun readFile(path: String): String? {
        Log.d(TAG, "Reading file: $path")
        return try {
            val file = File(path)
            if (file.exists()) {
                file.readText()
            } else {
                // Try cat via shell as fallback
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "cat \"$path\" 2>&1"))
                val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
                val exitCode = process.waitFor()
                if (exitCode == 0) output else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read file: $path", e)
            null
        }
    }

    override fun writeFile(path: String, content: String): Boolean {
        Log.d(TAG, "Writing file: $path (length: ${content.length})")
        val file = File(path)
        try {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            file.writeText(content)
            Log.d(TAG, "Successfully wrote file via Java IO: $path")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file via Java IO: $path. Trying shell fallback...", e)
            return try {
                // Shell fallback: mkdir -p first
                val parentPath = file.parent ?: "/storage/emulated/0"
                Runtime.getRuntime().exec(arrayOf("sh", "-c", "mkdir -p \"$parentPath\"")).waitFor()
                
                // Write via cat and stdin to avoid escaping nightmares
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "cat > \"$path\""))
                process.outputStream.use { it.write(content.toByteArray()) }
                val exitCode = process.waitFor()
                Log.d(TAG, "Shell write exit code: $exitCode")
                exitCode == 0
            } catch (e2: Exception) {
                Log.e(TAG, "Shell fallback also failed for $path", e2)
                false
            }
        }
    }

    override fun getLastModified(path: String): Long {
        return try {
            File(path).lastModified()
        } catch (e: Exception) {
            0L
        }
    }
}
