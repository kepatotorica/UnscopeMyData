package com.kepat.unscopemydata.data

import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import java.io.File

object ManifestManager {
    private const val TAG = "ManifestManager"
    private val gson = Gson()
    private val unscopedDir: File
        get() = File(Environment.getExternalStorageDirectory(), SettingsManager.basePath)
    
    private val manifestFile: File
        get() = File(unscopedDir, "manifest.json")

    fun getManifestLastModified(): Long {
        val shizukuModified = ShizukuFileManager.getLastModified(manifestFile.absolutePath)
        if (shizukuModified != 0L) return shizukuModified
        
        return try {
            if (manifestFile.exists()) manifestFile.lastModified() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun loadManifest(): Manifest {
        val path = manifestFile.absolutePath
        var json = ShizukuFileManager.readFile(path)
        
        if (json == null) {
            Log.w(TAG, "Failed to load manifest via Shizuku, trying local fallback.")
            json = try {
                if (manifestFile.exists()) manifestFile.readText() else null
            } catch (e: Exception) {
                Log.e(TAG, "Local fallback load failed: ${e.message}")
                null
            }
        }

        return if (json != null) {
            try {
                gson.fromJson(json, Manifest::class.java) ?: Manifest()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse manifest JSON", e)
                Manifest()
            }
        } else {
            Manifest()
        }
    }

    fun saveManifest(manifest: Manifest) {
        val json = gson.toJson(manifest)
        val path = manifestFile.absolutePath
        
        Log.d(TAG, "Attempting to save manifest via Shizuku...")
        if (!ShizukuFileManager.writeFile(path, json)) {
            Log.w(TAG, "Failed to save manifest via Shizuku. Possible ENOTCONN or permission issue.")
            
            // Local fallback attempt
            try {
                if (!unscopedDir.exists()) {
                    unscopedDir.mkdirs()
                }
                manifestFile.writeText(json)
                Log.d(TAG, "Local fallback save successful (unexpectedly, given Shizuku failed).")
            } catch (e: Exception) {
                // This is where ENOTCONN usually hits the local process
                Log.e(TAG, "Local fallback save failed as expected: ${e.message}")
            }
        } else {
            Log.d(TAG, "Manifest saved successfully via Shizuku.")
        }
    }
}
