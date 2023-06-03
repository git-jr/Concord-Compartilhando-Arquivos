package com.alura.concord.network

import android.accounts.NetworkErrorException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.UnknownHostException

object DownloadService {

    suspend fun makeDownloadByUrl(
        url: String,
        onFinisheDownload: (InputStream) -> Unit,
        onFailureDownload: () -> Unit
    ) {

        try {
            withContext(IO) {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .build()
                delay(1000)
                client.newCall(request).execute().let { response ->
                    response.body?.byteStream()?.let { fileData: InputStream ->
                        withContext(Main) {
                            onFinisheDownload(fileData)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            when (exception) {
                is NetworkErrorException,
                is UnknownHostException,
                is FileNotFoundException -> {
                    onFailureDownload()
                }

                else -> throw exception
            }
        }
    }
}




