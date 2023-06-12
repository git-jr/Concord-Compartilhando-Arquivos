package com.alura.concord.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.use
import java.io.File
import java.io.InputStream

object DownloadService {

    fun makeDownloadByURL(url: String, context: Context) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            response.body?.byteStream()?.use { fileData: InputStream ->
                val path = context.getExternalFilesDir("temp")
                val newFile = File(path, "Teste.png")

                newFile.outputStream().use { file ->
                    fileData.copyTo(file)
                }
            }
        }
    }
}