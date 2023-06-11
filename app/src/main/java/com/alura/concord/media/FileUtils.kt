package com.alura.concord.media

import android.content.Context
import android.content.Intent
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


suspend fun Context.saveOnInternalStorage(
    inputStream: InputStream,
    fileName: String,
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit
) {

    val path = getExternalFilesDir("temp")
    val newFile = File(path, fileName)

    withContext(IO) {
        newFile.outputStream().use { file ->
            inputStream.copyTo(file)
        }

        if (newFile.exists()) {
            onSuccess(newFile.path)
        } else {
            onFailure()
        }
    }
}


fun Context.openWith(mediaLink: String) {

    val file = File(mediaLink)
    val fileUri: Uri = getFileUriProvider(file)
    val fileMimeType = file.getMimeType()

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(fileUri, fileMimeType)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)

}

fun Context.shareFile(mediaLink: String) {

    val file = File(mediaLink)
    val fileUri: Uri = getFileUriProvider(file)

    val fileMimeType = file.getMimeType()

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, fileUri)
        type = fileMimeType
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)

}

private fun File.getMimeType(): String? {
    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(Uri.encode(this.path))
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
}

private fun Context.getFileUriProvider(file: File): Uri {
    return FileProvider.getUriForFile(
        this,
        "com.alura.concord.fileprovider",
        file
    )
}