package com.alura.concord.network

import android.content.Context
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID

fun Context.makeDownload(
    url: String,
    fileName: String = UUID.randomUUID().toString(),
    onFinisheDownload: (String) -> Unit,
    onFailDownload: () -> Unit
) {
    val folderName = "temp"
    val path = getExternalFilesDir(folderName)
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response ->
        response.body?.byteStream()?.use { input ->
            val target = File(path, fileName)

            target.outputStream().use { output ->
                input.copyTo(output)
                if (!target.exists()) {
                    onFailDownload()
                    return
                } else {
                    onFinisheDownload(Uri.fromFile(target).toString())
                }
            }
        }
    }
}