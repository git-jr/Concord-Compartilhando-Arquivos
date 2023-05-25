package com.alura.concord.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream


suspend fun makeDownlaodByUrl(
    url: String,
    onFinisheDownload: (InputStream) -> Unit,
    onFailureDownload: () -> Unit
) {
    withContext(IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().let { response ->
                response.body?.byteStream()?.let { inputSream ->
                    onFinisheDownload(inputSream)
                }
            }

        } catch (exception: Exception) {
            onFailureDownload()
            Log.e("Download error:", exception.toString())
        }
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
        newFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        if (newFile.exists()) {
            onSuccess(newFile.path.toString())
        } else {
            onFailure()
        }
    }
}