package com.alura.concord.media

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

fun Long.formatReadableFileSize(): String {
    val size = this
    val kilobyte = 1024
    val megaByte = kilobyte * 1024
    val gigaByte = megaByte * 1024

    return when {
        size < kilobyte -> "$size B"
        size < megaByte -> "${size / kilobyte} KB"
        size < gigaByte -> "${size / megaByte} MB"
        else -> "${size / gigaByte} GB"
    }
}


suspend fun Context.saveFileOnInternalStorage(
    inputStream: InputStream,
    fileName: String,
    onSuccess: (String) -> Unit,
) {
    val folderName = "temp"
    val path = getExternalFilesDir(folderName)
    val newFile = File(path, fileName)

    withContext(IO) {
        newFile.outputStream().use { file ->
            inputStream.copyTo(file)
        }
        
        if (newFile.exists()) {
            onSuccess(newFile.path.toString())
        }
    }
}