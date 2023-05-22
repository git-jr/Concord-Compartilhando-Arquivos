package com.alura.concord.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.alura.concord.extensions.showLog
import com.alura.concord.extensions.showMessage
import com.alura.concord.media.getAllImages
import com.alura.concord.media.getNameByUri
import com.alura.concord.media.imagePermission
import com.alura.concord.media.persistUriPermission
import com.alura.concord.media.verifyPermission
import com.alura.concord.network.makeDownload
import com.alura.concord.ui.chat.MessageListViewModel
import com.alura.concord.ui.chat.MessageScreen
import com.alura.concord.ui.components.ModalBottomSheetFile
import com.alura.concord.ui.components.ModalBottomSheetShare
import com.alura.concord.ui.components.ModalBottomSheetSticker
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


internal const val messageChatRoute = "messages"
internal const val messageChatIdArgument = "chatId"
internal const val messageChatFullPath = "$messageChatRoute/{$messageChatIdArgument}"


fun NavGraphBuilder.messageListScreen(
    onBack: () -> Unit = {},
) {
    composable(messageChatFullPath) { backStackEntry ->
        backStackEntry.arguments?.getString(messageChatIdArgument)?.let { chatId ->
            val viewModelMessage = hiltViewModel<MessageListViewModel>()
            val uiState by viewModelMessage.uiState.collectAsState()
            val context = LocalContext.current

            val requestPermissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        viewModelMessage.setShowBottomSheetSticker(true)
                    } else {
                        context.showMessage(
                            "Permissão não concedida, não será possivel acessar os stickers sem ela",
                            true
                        )
                    }
                }

            val scope = rememberCoroutineScope { IO }

            var selectMessage: Boolean = false
            MessageScreen(
                state = uiState,
                onSendMessage = {
                    viewModelMessage.sendMessage()
                },
                onShowSelectorFile = {
                    viewModelMessage.setShowBottomSheetFile(true)
                },
                onShowSelectorStickers = {
                    if (context.verifyPermission(imagePermission())) {
                        requestPermissionLauncher.launch(imagePermission())
                    } else {
                        viewModelMessage.setShowBottomSheetSticker(true)
                    }
                },
                onDeselectMedia = {
                    viewModelMessage.deselectMedia()
                },
                onBack = {
                    onBack()
                },
                onContentDownload = { message ->
                    viewModelMessage.startDownload(message.id)

                    message.downloadableContent?.let { content ->
                        scope.launch {
                            makeDownload(
                                context,
                                content.url,
                                "Arquivo.png",
                                onFinisheDownload = { contentPath ->
                                    context.showLog("Quando acabar os testes, chamar MessageDAO para atualizar direto no banco")
                                    viewModelMessage.finishDownload(message.id, contentPath)
                                },
                                onFailDownload = {
                                    viewModelMessage.failDownload(message.id)
                                })
                        }
                    }
                },
                onShowFileOptions = { selectedMessage ->
//                    selectMessage = selectedMessage.isSelected
//                    selectedMessage.isSelected = selectMessage

                    viewModelMessage.setShowFileOptions(selectedMessage, true)
                }
            )

            if (uiState.showDialogFileOptions) {
                DialogShareFile(viewModelMessage)
            }

            if (uiState.showBottomSheetSticker) {

                val stickerList = mutableStateListOf<String>()

                context.getAllImages(onLoadImages = { images ->
                    stickerList.addAll(images)
                })

                ModalBottomSheetSticker(
                    stickerList = stickerList,
                    onSelectedSticker = {
                        viewModelMessage.setShowBottomSheetSticker(false)
                        viewModelMessage.loadMediaInScreen(path = it.toString())
                        viewModelMessage.sendMessage()
                    }, onBack = {
                        viewModelMessage.setShowBottomSheetSticker(false)
                    })
            }

            val pickMedia = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    context.persistUriPermission(uri)

                    viewModelMessage.loadMediaInScreen(uri.toString())
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            }

            val pickFile = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    context.persistUriPermission(uri)

                    val name = context.getNameByUri(uri)

                    uiState.onMessageValueChange(name.toString())
                    viewModelMessage.loadMediaInScreen(uri.toString())
                    viewModelMessage.sendMessage()
                } else {
                    Log.d("FilePicker", "No media selected")
                }
            }

            if (uiState.showBottomSheetFile) {
                ModalBottomSheetFile(
                    onSelectPhoto = {
                        pickMedia.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                            )
                        )
                        viewModelMessage.setShowBottomSheetFile(false)
                    },
                    onSelectFile = {
                        pickFile.launch(arrayOf("*/*"))
                        viewModelMessage.setShowBottomSheetFile(false)
                    }, onBack = {
                        viewModelMessage.setShowBottomSheetFile(false)
                    })
            }

            val choseFolderResultLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("*/*"),
                onResult = {
                    it?.let { path ->

                        var mediaToOpen = uiState.selectedMessage.mediaLink

                        mediaToOpen =
                            "/storage/emulated/0/Android/data/com.alura.concord/files/temp/Documento.pdf"

                        mediaToOpen =
                            "/storage/emulated/0/Android/data/com.alura.concord/files/temp/Arquivo.png"

                        scope.launch {
                            moveFile(
                                originalPathFile = mediaToOpen,
                                destinationFile = path,
                                context = context
                            )
                        }
                    }
                }
            )


            if (uiState.showBottomSheetShare) {
                var mediaToOpen = uiState.selectedMessage.mediaLink

                mediaToOpen =
                    "/storage/emulated/0/Android/data/com.alura.concord/files/temp/Arquivo.png"

                mediaToOpen =
                    "/storage/emulated/0/Android/data/com.alura.concord/files/temp/Documento.pdf"


                ModalBottomSheetShare(
                    onOpenWith = {
                        viewModelMessage.setShowBottomSheetShare(false)
                        openWith(mediaToOpen, context)
                    },
                    onShare = {
                        shareFile(mediaToOpen, context)
                        viewModelMessage.setShowBottomSheetShare(false)
                    },
                    onSave = {
                        var nameFile =
                            context.getNameByUri(Uri.parse(mediaToOpen))

                        // Isso aqui só para os teste aonde não temo como passar a URI em si,
                        // na versão final provavelmente vai será só context.getNameByUri() mesmo
                        nameFile = File(mediaToOpen).name

                        choseFolderResultLauncher.launch(nameFile)
                        viewModelMessage.setShowBottomSheetShare(false)
                    },
                    onBack = {
                        viewModelMessage.setShowBottomSheetShare(false)
                    })
            }
        }
    }
}

private fun openWith(
    mediaLink: String,
    context: Context
) {
    val file = File(mediaLink)
    val fileUri = FileProvider.getUriForFile(
        context,
        "com.alura.concord.fileprovider",
        file
    )

    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(file.path)
    val mimeTypeFromFile =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(fileUri, "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Abrir com")
    context.startActivity(chooserIntent)
}


private fun shareFile(
    mediaLink: String,
    context: Context
) {
    val file = File(mediaLink)
    val fileUri = FileProvider.getUriForFile(
        context,
        "com.alura.concord.fileprovider",
        file
    )

    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(file.path)
    val mimeTypeFromFile =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, fileUri)
        setDataAndType(fileUri, "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Compartilhar")
    context.startActivity(chooserIntent)
}


private fun moveFile(originalPathFile: String, destinationFile: Uri, context: Context) {
    try {
        val originalFile =
            File(originalPathFile)

        val inputStream = FileInputStream(originalFile)
        val contentResolver = context.contentResolver
        val outputStream = contentResolver.openOutputStream(destinationFile)

        inputStream.use { input ->
            outputStream.use { output ->
                output?.let { input.copyTo(it) }
            }
        }


//        // Depois de criar o novo arquivo, vamos apagar o antigo
//        if (originalFile.delete()) {
//            Log.d("File deletion", "Success! Original file deleted")
//        } else {
//            Log.e("File deletion", "Failed to delete original file")
//        }

    } catch (e: Exception) {
        // Tratamento de erro
        Log.e("File move error", e.toString())
    }
}

@Composable
private fun DialogShareFile(viewModelMessage: MessageListViewModel) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope { IO }

    val choseFolderToSaveresultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = {
            it?.let { path ->
                scope.launch {

//                    saveFileIn(context, path)
                    moveFileBase(path, context)
                }
            }
        }
    )

    Dialog(onDismissRequest = {
        viewModelMessage.setShowFileOptions(show = false)
    }) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .clip(RoundedCornerShape(50.dp))
                .defaultMinSize(minHeight = 100.dp)
                .padding(16.dp)
        ) {
            Text(
                text = "Selecione uma opção",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Abrir com",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
                    .background(Color.LightGray)
                    .clip(RoundedCornerShape(8.dp))
                    .defaultMinSize(minHeight = 50.dp)
                    .padding(8.dp)
                    .clickable {
                        viewModelMessage.setShowFileOptions(show = false)


                        val file = File(context.getExternalFilesDir("temp"), "Arquivo.png")
                        val fileUri = FileProvider.getUriForFile(
                            context,
                            "com.alura.concord.fileprovider",
                            file
                        )

                        val shareIntent: Intent = Intent().apply {
                            action = Intent.ACTION_VIEW
                            setDataAndType(fileUri, "image/png")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        val chooserIntent = Intent.createChooser(shareIntent, "Abrir com")
                        context.startActivity(chooserIntent)

                    }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Compartilhar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
                    .background(Color.LightGray)
                    .clip(RoundedCornerShape(8.dp))
                    .defaultMinSize(minHeight = 50.dp)
                    .padding(8.dp)
                    .clickable {
                        var mediaLink =
                            "/storage/self/primary/Android/data/com.alura.concord/files/temp/Documento.pdf"
                        mediaLink =
                            "/storage/self/primary/Android/data/com.alura.concord/files/temp/Arquivo.png"
//                        val file = File(context.getExternalFilesDir("temp"), "Arquivo.png")
//                        val file = File(
//                            mediaLink,
//                            "Arquivo.png"
//                        )
                        val file = File(mediaLink)
                        val fileUri: Uri = FileProvider.getUriForFile(
                            context,
                            "com.alura.concord.fileprovider", // Provedor declarado no manifest
                            file
                        )

                        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(file.path)
                        val mimeTypeFromFile =
                            MimeTypeMap
                                .getSingleton()
                                .getMimeTypeFromExtension(fileExtension)


                        val shareIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, fileUri)
//                            type = "*/*"
                            type = mimeTypeFromFile

                        }

                        val chooserIntent = Intent.createChooser(shareIntent, null)
                        chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Concendo acesso como já foi visto no curso 1 de arquivos
                        context.startActivity(chooserIntent)


//                        val shareIntent: Intent = Intent().apply {
//                            action = Intent.ACTION_SEND
//                            // Example: content://com.google.android.apps.photos.contentprovider/...
//                            putExtra(Intent.EXTRA_STREAM,"/storage/self/primary/Android/data/com.teste.teste/files/temp/Arquivo.png")
//                            type = "image/png"
//                        }
//                       context.startActivity(Intent.createChooser(shareIntent, null))
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Salvar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
                    .background(Color.LightGray)
                    .clip(RoundedCornerShape(8.dp))
                    .defaultMinSize(minHeight = 50.dp)
                    .padding(8.dp)
                    .clickable {
                        // Dar a opção de escolher o local para salvar o arquivo
                        choseFolderToSaveresultLauncher.launch("Arquivo.png")
                    }
            )
        }
    }
}

fun saveFileIn(pathSaveFile: Uri, context: Context) {
    val contentResolver = context.contentResolver
}

suspend fun saveFileIn(context: Context, destinationUri: Uri): Boolean {

    val sourceUri =
        Uri.parse("/storage/self/primary/Android/data/com.alura.concord/files/temp/Arquivo.png")

    val inputStream = context.contentResolver.openInputStream(sourceUri)
    val outputStream = context.contentResolver.openOutputStream(destinationUri)

    if (inputStream != null && outputStream != null) {
        try {
            inputStream.copyTo(outputStream)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream.close()
            outputStream.close()
        }
    }

    return false
}


fun moveFileBase(destinationUri: Uri, context: Context) {
    try {
        val fileToMove =
            File("/storage/self/primary/Android/data/com.alura.concord/files/temp/Arquivo.png")
        val inputStream = FileInputStream(fileToMove)
        val contentResolver = context.contentResolver
        val outputStream = contentResolver.openOutputStream(destinationUri)

        inputStream.use { input ->
            outputStream.use { output ->
                output?.let { input.copyTo(it) }
            }
        }


        // Depois de criar o novo arquivo, vamos apagar o antigo
        if (!fileToMove.delete()) {
            Log.e("File deletion error", "Failed to delete original file")
        }

    } catch (e: Exception) {
        // Tratamento de erro
        Log.e("File move error", e.toString())
    }
}


suspend fun moveUmArquivoFuncionalOld(): Boolean {
    val path = "/storage/self/primary/Documents/Nova/Arquivo.png"
    val destinationFilePath = path // destinationUri.path ?: return false
    val destinationFile = File(destinationFilePath)

    val sourceFile =
        File("/storage/self/primary/Android/data/com.alura.concord/files/temp/Arquivo.png")

    return try {
        // Verificar se o arquivo de destino já existe e excluí-lo
//        if (destinationFile.exists()) {
//            destinationFile.delete()
//        }

        // Mover o arquivo de origem para o destino
        sourceFile.copyTo(destinationFile, overwrite = true)

        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}

internal fun NavHostController.navigateToMessageScreen(
    chatId: Long,
    navOptions: NavOptions? = null
) {
    navigate("$messageChatRoute/$chatId", navOptions)
}

