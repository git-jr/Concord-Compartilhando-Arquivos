package com.alura.concord.network

import android.accounts.NetworkErrorException
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.UnknownHostException

object DownloadService {
    suspend fun makeDownloadByUrl(
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
                        withContext(Main) {
                            onFinisheDownload(inputSream)
                        }
                    }
                }

            } catch (exception: Exception) {
                when (exception) {
                    is UnknownHostException,
                    is NetworkErrorException,
                    is FileNotFoundException -> {
                        onFailureDownload()
                        Log.e("Download error:", exception.toString())
                    }

                    else -> throw exception
                }
            }
        }
    }
}
