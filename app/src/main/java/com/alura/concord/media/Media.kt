package com.alura.concord.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

const val FILE_INFO = "FILE"

fun Context.getAllImages(onLoadImages: (List<String>) -> Unit) {
    val images = mutableListOf<String>()

    val projection = arrayOf(
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATA,
    )
    val selection = "${MediaStore.Images.Media.DATA} LIKE '%/Download/stickers/%' " +
            "AND ${MediaStore.Images.Media.SIZE} > ?"
    val selectionArgs = arrayOf("70000")
    val sortOrder = "${MediaStore.Images.Media.DISPLAY_NAME} DESC"

    contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->

        while (cursor.moveToNext()) {
            val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            val path = cursor.getString(pathIndex)

            images.add(path)
        }
        onLoadImages(images)
    }
}


fun Context.getNameByUri(uri: Uri): String? {
    return contentResolver.query(uri, null, null, null, null)
        .use { cursor ->
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor?.moveToFirst()
            nameIndex?.let { cursor.getString(it) }
        }
}


fun Context.openWith(
    mediaLink: String
) {
    val file = File(mediaLink)
    val fileUri = FileProvider.getUriForFile(
        this,
        "com.alura.concord.fileprovider",
        file
    )

    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(file.path)
    val mimeTypeFromFile =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

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
    val fileUri = FileProvider.getUriForFile(
        this,
        "com.alura.concord.fileprovider",
        file
    )

    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(file.path)
    val mimeTypeFromFile =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

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

fun formatReadableFileSize(size: Long): String {
    val kilobyte = 1024
    val megaByte = kilobyte * 1024

    return when {
        size < kilobyte -> {
            "$size KB"
        }

        size < megaByte -> {
            "${size / 1024} MB"
        }

        else -> {
            "${size / (1024 * 1024)} GB"
        }
    }
}