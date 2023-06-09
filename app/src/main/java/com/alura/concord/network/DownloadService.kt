package com.alura.concord.network

import android.content.Context
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream

object DownloadService {

    suspend fun makeDownloadFileByURL(url: String, context: Context) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        withContext(IO) {
            client.newCall(request).execute().let { response ->
                response.body?.byteStream()?.let { fileData: InputStream ->
                    val path = context.getExternalFilesDir("temp")
                    val newFile = File(path, "Arquivo teste.png")

                    newFile.outputStream().use { file ->
                        fileData.copyTo(file)
                    }
                }
            }
        }
    }
}