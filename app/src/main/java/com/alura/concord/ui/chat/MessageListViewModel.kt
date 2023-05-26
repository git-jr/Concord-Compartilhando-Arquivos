package com.alura.concord.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alura.concord.data.Author
import com.alura.concord.data.DownloadStatus
import com.alura.concord.data.FileInDownload
import com.alura.concord.data.MessageEntity
import com.alura.concord.data.MessageWithFile
import com.alura.concord.data.toDownloadableFile
import com.alura.concord.data.toMessageEntity
import com.alura.concord.data.toMessageFile
import com.alura.concord.database.ChatDao
import com.alura.concord.database.DownloadableFileDao
import com.alura.concord.database.MessageDao
import com.alura.concord.navigation.messageChatIdArgument
import com.alura.concord.network.DownloadService
import com.alura.concord.network.DownloadService.makeDownloadByUrl
import com.alura.concord.util.getFormattedCurrentDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val downloadableFileDao: DownloadableFileDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(MessageListUiState())
    val uiState: StateFlow<MessageListUiState>
        get() = _uiState.asStateFlow()

    private var chatId: Long =
        requireNotNull(savedStateHandle.get<String>(messageChatIdArgument)?.toLong())

    init {
        loadDatas()

        _uiState.update { state ->
            state.copy(
                onMessageValueChange = {
                    _uiState.value = _uiState.value.copy(
                        messageValue = it
                    )

                    _uiState.value = _uiState.value.copy(
                        hasContentToSend = (it.isNotEmpty() || _uiState.value.mediaInSelection.isNotEmpty())
                    )
                },

                onMediaInSelectionChange = {
                    _uiState.value = _uiState.value.copy(
                        mediaInSelection = it
                    )
                    _uiState.value = _uiState.value.copy(
                        hasContentToSend = (it.isNotEmpty() || _uiState.value.messageValue.isNotEmpty())
                    )
                }
            )
        }
    }

    private fun loadDatas() {
        loadChatsInfos()
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            messageDao.getByChatId(chatId).collect { messages ->
                messages.forEach { searchedMessage ->
                    if (searchedMessage.author == Author.OTHER) {
                        loadMessageWithDownloadableContent(
                            searchedMessage.toMessageFile()
                        )?.let { messageWithDownloadableContent ->
                            _uiState.value = _uiState.value.copy(
                                messages = _uiState.value.messages + messageWithDownloadableContent
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + searchedMessage.toMessageFile()
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadMessageWithDownloadableContent(
        searchedMessage: MessageWithFile
    ): MessageWithFile? {
        return searchedMessage.idDownloadableFile?.let { contentId ->
            val downloadableFileEntity = downloadableFileDao.getById(contentId).first()
            searchedMessage.copy(
                downloadableFile = downloadableFileEntity?.toDownloadableFile()
            )
        }
    }

    private fun loadChatsInfos() {
        viewModelScope.launch {
            val chat = chatDao.getById(chatId).first()
            chat?.let {
                _uiState.value = _uiState.value.copy(
                    ownerName = chat.owner,
                    profilePicOwner = chat.profilePicOwner
                )
            }
        }
    }

    private fun saveMessage(
        userMessage: MessageEntity
    ) {
        viewModelScope.launch {
            userMessage.let { messageDao.insert(it) }
        }
    }

    private fun cleanFields() {
        _uiState.value = _uiState.value.copy(
            messageValue = "",
            mediaInSelection = "",
            hasContentToSend = false
        )
    }

    fun sendMessage() {
        with(_uiState) {
            if (!value.hasContentToSend) {
                return
            }

            val userMessageEntity = MessageEntity(
                content = value.messageValue,
                author = Author.USER,
                chatId = chatId,
                mediaLink = value.mediaInSelection,
                date = getFormattedCurrentDate(),
            )
            saveMessage(userMessageEntity)
            cleanFields()
        }
    }

    fun loadMediaInScreen(
        path: String
    ) {
        _uiState.value.onMediaInSelectionChange(path)
    }

    fun deselectMedia() {
        _uiState.value = _uiState.value.copy(
            mediaInSelection = "",
            hasContentToSend = false
        )
    }

    fun setImagePermission(value: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasImagePermission = value,
        )
    }


    fun setShowBottomSheetSticker(value: Boolean) {
        _uiState.value = _uiState.value.copy(
            showBottomSheetSticker = value,
        )
    }

    fun setShowBottomSheetFile(value: Boolean) {
        _uiState.value = _uiState.value.copy(
            showBottomSheetFile = value,
        )
    }


    fun setShowBottomSheetShare(value: Boolean) {
        _uiState.value = _uiState.value.copy(
            showBottomSheetShare = value,
        )
    }


    fun setShowFileOptions(message: MessageWithFile? = null, show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showBottomSheetShare = show,
            selectedMessage = message ?: MessageWithFile()
        )
    }

    fun startDownload(messageWithDownload: MessageWithFile) {

        val updatedMessages = _uiState.value.messages.map { message ->
            if (message.id == messageWithDownload.id) {
                message.copy(
                    downloadableFile = message.downloadableFile?.copy(
                        status = DownloadStatus.DOWNLOADING
                    )
                )
            } else {
                message
            }
        }

        val fileInDownload = messageWithDownload.downloadableFile?.let {
            FileInDownload(
                messageId = messageWithDownload.id,
                url = it.url,
                name = it.name,
                inputStream = null
            )
        }

        fileInDownload?.let {
            _uiState.value = _uiState.value.copy(
                messages = updatedMessages,
                fileInDownload = fileInDownload
            )
            makeDownload(fileInDownload)
        }

    }

    private fun makeDownload(
        fileInDownload: FileInDownload,
    ) = viewModelScope.launch {
        delay(5000)
        makeDownloadByUrl(
            url = fileInDownload.url,
            onFinisheDownload = { inputStream ->
                _uiState.value = _uiState.value.copy(
                    fileInDownload = _uiState.value.fileInDownload?.copy(
                        inputStream = inputStream
                    )
                )
            },
            onFailureDownload = {
                failDownload(fileInDownload.messageId)
            }
        )
    }

    fun finishDownload(messageId: Long, contentPath: String) {
        _uiState.value.messages.map { message ->
            if (message.id == messageId) {
                val messageWithoutContentDownload = message.copy(
                    idDownloadableFile = 0,
                    downloadableFile = null,
                    mediaLink = contentPath,
                )
                updateSingleMessage(messageWithoutContentDownload.toMessageEntity())
            } else {
                message
            }
        }
        _uiState.value = _uiState.value.copy(fileInDownload = null)
    }

    fun failDownload(messageId: Long) {
        val updatedMessages = _uiState.value.messages.map { message ->
            if (message.id == messageId) {
                message.copy(
                    downloadableFile = message.downloadableFile?.copy(
                        status = DownloadStatus.ERROR
                    )
                )
            } else {
                message
            }
        }

        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            fileInDownload = null
        )
    }

    private fun updateSingleMessage(messageEntity: MessageEntity) {
        viewModelScope.launch {
            messageDao.insert(messageEntity)
        }
    }


    fun downloadInProgress(): Boolean {
        return _uiState.value.fileInDownload == null
    }
}