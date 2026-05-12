package com.kepat.unscopemydata.data

import android.os.Environment
import com.google.gson.Gson
import java.io.File

object ManifestManager {
    private val gson = Gson()
    private val unscopedDir: File
        get() = File(Environment.getExternalStorageDirectory(), SettingsManager.basePath)
    
    private val manifestFile: File
        get() = File(unscopedDir, "manifest.json")

    fun getManifestLastModified(): Long {
        return if (manifestFile.exists()) manifestFile.lastModified() else 0L
    }

    fun loadManifest(): Manifest {
        if (!unscopedDir.exists()) {
            unscopedDir.mkdirs()
        }
        if (!manifestFile.exists()) {
            return Manifest()
        }
        return try {
            val json = manifestFile.readText()
            gson.fromJson(json, Manifest::class.java) ?: Manifest()
        } catch (e: Exception) {
            e.printStackTrace()
            Manifest()
        }
    }

    fun saveManifest(manifest: Manifest) {
        if (!unscopedDir.exists()) {
            unscopedDir.mkdirs()
        }
        val json = gson.toJson(manifest)
        manifestFile.writeText(json)
    }
}
