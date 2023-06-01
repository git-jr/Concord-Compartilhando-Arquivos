package com.alura.concord.network

import android.content.Context
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object DownloadService {

    suspend fun makeDownloadByUrl(
        url: String,
        context: Context
    ) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        withContext(IO) {
            client.newCall(request).execute().let { response ->
                response.body?.byteStream()?.let { fileData ->
                    val path = context.getExternalFilesDir("temp")
                    val newFile = File(path, "Novo arquivo.png")

                    newFile.outputStream().use { file ->
                        fileData.copyTo(file)
                    }
                }
            }
        }
    }
}

