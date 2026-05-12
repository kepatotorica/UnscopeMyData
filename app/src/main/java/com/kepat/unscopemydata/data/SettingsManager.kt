package com.kepat.unscopemydata.data

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "unscopemydata_settings"
    private const val KEY_BASE_PATH = "base_path"
    private const val DEFAULT_BASE_PATH = ".xyz/UnscopeMyData"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var basePath: String
        get() = prefs.getString(KEY_BASE_PATH, DEFAULT_BASE_PATH) ?: DEFAULT_BASE_PATH
        set(value) {
            prefs.edit().putString(KEY_BASE_PATH, value).apply()
        }
}
