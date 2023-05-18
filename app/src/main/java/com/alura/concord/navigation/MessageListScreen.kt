package com.alura.concord.navigation

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
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
import com.alura.concord.network.fileMore1MB
import com.alura.concord.network.fileMore700MB
import com.alura.concord.network.makeDownload
import com.alura.concord.ui.chat.MessageListViewModel
import com.alura.concord.ui.chat.MessageScreen
import com.alura.concord.ui.components.ModalBottomSheetFile
import com.alura.concord.ui.components.ModalBottomSheetSticker
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

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
            )


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
        }
    }
}


internal fun NavHostController.navigateToMessageScreen(
    chatId: Long,
    navOptions: NavOptions? = null
) {
    navigate("$messageChatRoute/$chatId", navOptions)
}

