package com.alura.concord.network

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url
import java.io.File
import java.util.UUID


fun gitFileSizeInKB(url: String, callback: (Long?) -> Unit) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://github.com/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()

    val fileApi = retrofit.create(FileApi::class.java)

    fileApi.getFileSize(url, "bytes=0-0").enqueue(object : retrofit2.Callback<String> {
        override fun onResponse(
            call: retrofit2.Call<String>,
            response: retrofit2.Response<String>
        ) {
            val temp = response.headers()["Content-Range"]
            val header = response.headers()

            val fileSize =
                response.headers()["Content-Range"]?.substringAfterLast('/')?.toLongOrNull()
                    ?: -1L
            callback(fileSize / 1024)
        }

        override fun onFailure(call: retrofit2.Call<String>, t: Throwable) {
            callback(null)
        }
    })
}


private interface FileApi {
    @GET
    fun getFileSize(@Url url: String, @Header("Range") range: String): retrofit2.Call<String>
}


const val file7KB =
    "https://github.com/git-jr/sample-files/raw/main/stickers/Emoji%207%20Android%20Ice%20Cream.png"
const val file92KB =
    "https://github.com/git-jr/sample-files/raw/main/audios/%C3%81udio%20teste%201.mp3"
const val fileMore1MB = "https://github.com/git-jr/sample-files/raw/main/documents/Realatorio.pdf"
const val fileMore700MB =
    "https://download1654.mediafire.com/hfq3t5bcncogN7WJLOZ05HidQUJ_qcRxq0fFbdM5qYF2DJAGunbXJ-8HNUawhVl9UhIfAXSoOFhOXYScGgL7Qv00AAN7lr9KM95V0kkDkhbhfB_CNfmLQOu-qJPsbOk3PoSUoT24OXUxHFuYVhVlOez6ZadaZGQ9wMc7qouknDg8HA/x92f6bwfas1n703/Projetos+e+M%C3%ADdias+layout+Paradoxo+2021.zip"
const val fileInGB =
    "https://releases.ubuntu.com/22.04.2/ubuntu-22.04.2-desktop-amd64.iso?_ga=2.6087732.66032191.1684249590-1145850122.1684249590"


fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> {
            "$size KB"
        }

        size < 1024 * 1024 -> {
            "${size / 1024} MB"
        }

        else -> {
            "${size / (1024 * 1024)} GB"
        }
    }
}


fun makeDownloadDownloadManager(context: Context, url: String) {
    val fileName = "Arquivo de teste documento.pdf"
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Downloading $fileName")
        .setDescription("Downloading...")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = downloadManager.enqueue(request)
}


suspend fun makeDownload(
    contetx: Context,
    url: String,
    fileName: String = UUID.randomUUID().toString(),
    onFinisheDownload: (String) -> Unit,
    onFailDownload: () -> Unit
) {
    val path = contetx.getExternalFilesDir("temp")
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