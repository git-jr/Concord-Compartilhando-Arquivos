package com.alura.concord.media

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
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
    onFailure: () -> Unit
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
        } else {
            onFailure()
        }
    }
}


fun Context.openFileWith(mediaLink: String) {

    val file = File(mediaLink)
    val fileUri = getFileProviderUri(file)

    val mimeType = file.getMimeType()

    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(fileUri, mimeType)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    startActivity(Intent.createChooser(shareIntent, "Abrir com"))
}

fun Context.shareFile(mediaLink: String) {

    val file = File(mediaLink)
    val fileUri = getFileProviderUri(file)
    val mimeType = file.getMimeType()

    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = mimeType
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        putExtra(EXTRA_STREAM, fileUri)
    }
    startActivity(Intent.createChooser(shareIntent, "Compartilhar"))
}

private fun Context.getFileProviderUri(file: File): Uri? {
    return FileProvider.getUriForFile(
        this,
        "com.alura.concord.fileprovider",
        file
    )
}

private fun File.getMimeType(): String {
    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(Uri.encode(path))
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension) ?: "*/*"
}