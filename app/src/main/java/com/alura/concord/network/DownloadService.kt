package com.alura.concord.network

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream

object DownloadService {

    suspend fun makeDownloadByUrl(
        url: String,
        onFinisheDownload: (InputStream) -> Unit,
    ) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        withContext(IO) {
            client.newCall(request).execute().let { response ->
                response.body?.byteStream()?.let { fileData: InputStream ->
                    onFinisheDownload(fileData)
                }
            }
        }
    }
}

