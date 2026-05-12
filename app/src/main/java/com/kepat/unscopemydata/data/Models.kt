package com.kepat.unscopemydata.data

data class SyncFolder(
    val dataFolderName: String,
    val path: String,
    var mostRecentUpdator: String,
    var dateMovedFromDataToUnscoped: String = "Never"
)

data class Manifest(
    val folders: MutableList<SyncFolder> = mutableListOf()
)
