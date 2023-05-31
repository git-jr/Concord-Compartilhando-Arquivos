package com.alura.concord.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream


private const val FILE_INFO = "FILE"

private fun File.getFileMimeType(): String? {
    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(path)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
}

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

    withContext(Dispatchers.IO) {
        newFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        withContext(Dispatchers.Main) {
            if (newFile.exists()) {
                onSuccess(newFile.path.toString())
            } else {
                onFailure()
            }
        }
    }
}


fun Context.openFileWith(
    mediaLink: String
) {
    val file = File(mediaLink)
    val fileUri = getUriFileProvider(file)

    val mimeTypeFromFile = file.getFileMimeType()

    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(fileUri, mimeTypeFromFile)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Abrir com")
    this.startActivity(chooserIntent)
}


fun Context.shareFile(
    mediaLink: String
) {
    val file = File(mediaLink)
    val fileUri = getUriFileProvider(file)

    val mimeTypeFromFile = file.getFileMimeType()

    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, fileUri)
        setDataAndType(fileUri, mimeTypeFromFile)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Compartilhar")
    this.startActivity(chooserIntent)
}


fun Context.moveFile(
    sourcePathFile: String,
    destinationFile: Uri
) {
    try {
        val fileToMove = File(sourcePathFile)

        val inputStream = FileInputStream(fileToMove)
        val contentResolver = this.contentResolver
        val outputStream = contentResolver.openOutputStream(destinationFile)

        inputStream.use { input ->
            outputStream?.use { output ->
                output.let { input.copyTo(it) }
            }
        }

        if (fileToMove.delete()) {
            Log.i(FILE_INFO, "Success! Original file deleted")
        } else {
            Log.i(FILE_INFO, "Failed to delete original file")
        }

    } catch (e: FileNotFoundException) {
        Log.i(FILE_INFO, "File not found: ${e.message}")
    }
}

private fun Context.getUriFileProvider(file: File): Uri? {
    return FileProvider.getUriForFile(
        this,
        "com.alura.concord.fileprovider",
        file
    )
}
