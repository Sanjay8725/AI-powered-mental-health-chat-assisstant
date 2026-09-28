package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.DatasetItemEntity
import com.example.data.local.entity.KnowledgeResourceEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MoodEntity
import com.example.data.local.entity.UserEntity
import com.example.data.remote.GeminiApiService
import com.example.data.remote.SupabaseClient
import com.example.data.repository.MindCareRepository
import com.example.data.util.CsvParseResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab {
    CHAT,
    MOOD,
    KNOWLEDGE,
    DATASET,
    AUTH
}

data class DatasetUiState(
    val isUploading: Boolean = false,
    val lastImportStats: CsvParseResult? = null,
    val uploadError: String? = null,
    val searchQuery: String = "",
    val filterCategory: String = "ALL",
    val isRealtimeSyncing: Boolean = false,
    val syncStatusMessage: String? = null
)

class MindCareViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val supabaseClient = SupabaseClient(application)
    private val geminiService = GeminiApiService()

    val repository = MindCareRepository(
        appDao = database.appDao(),
        supabaseClient = supabaseClient,
        geminiService = geminiService
    )

    private val _currentTab = MutableStateFlow(ScreenTab.CHAT)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Safety / Crisis state
    private val _showCrisisModal = MutableStateFlow(false)
    val showCrisisModal: StateFlow<Boolean> = _showCrisisModal.asStateFlow()

    // Breathing exercise modal
    private val _showBreathingModal = MutableStateFlow(false)
    val showBreathingModal: StateFlow<Boolean> = _showBreathingModal.asStateFlow()

    // Moods
    private val _moods = MutableStateFlow<List<MoodEntity>>(emptyList())
    val moods: StateFlow<List<MoodEntity>> = _moods.asStateFlow()

    val knowledgeResources: StateFlow<List<KnowledgeResourceEntity>> = repository.knowledgeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val datasetItems: StateFlow<List<DatasetItemEntity>> = repository.datasetItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val datasetCount: StateFlow<Int> = repository.datasetCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _datasetUiState = MutableStateFlow(DatasetUiState())
    val datasetUiState: StateFlow<DatasetUiState> = _datasetUiState.asStateFlow()

    // Auth screen state
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccess = MutableStateFlow<String?>(null)
    val authSuccess: StateFlow<String?> = _authSuccess.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // General banner / toast message
    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage: StateFlow<String?> = _snackBarMessage.asStateFlow()

    private var messagesJob: Job? = null
    private var conversationsJob: Job? = null
    private var moodsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeSeeds()
            // Listen to current user changes
            currentUser.collect { user ->
                if (user != null) {
                    observeUserData(user.id)
                }
            }
        }
        startPeriodicRealtimeSync()
    }

    private fun observeUserData(userId: String) {
        conversationsJob?.cancel()
        conversationsJob = viewModelScope.launch {
            repository.getConversationsFlow(userId).collect { convs ->
                _conversations.value = convs
                if (_currentConversationId.value == null && convs.isNotEmpty()) {
                    selectConversation(convs.first().id)
                } else if (convs.isEmpty()) {
                    // Create first conversation
                    val newConv = repository.createConversation(userId)
                    selectConversation(newConv.id)
                }
            }
        }

        moodsJob?.cancel()
        moodsJob = viewModelScope.launch {
            repository.getMoodsFlow(userId).collect { list ->
                _moods.value = list
            }
        }
    }

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun selectConversation(conversationId: String) {
        _currentConversationId.value = conversationId
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessagesFlow(conversationId).collect { msgList ->
                _messages.value = msgList
            }
        }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val userId = currentUser.value?.id ?: "guest_user"
            val conv = repository.createConversation(userId, "New Reflection")
            selectConversation(conv.id)
            setTab(ScreenTab.CHAT)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_currentConversationId.value == id) {
                val next = conversations.value.firstOrNull { it.id != id }
                if (next != null) {
                    selectConversation(next.id)
                } else {
                    _currentConversationId.value = null
                    val userId = currentUser.value?.id ?: "guest_user"
                    val created = repository.createConversation(userId)
                    selectConversation(created.id)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return

        val convId = _currentConversationId.value
        val userId = currentUser.value?.id ?: "guest_user"

        if (convId == null) {
            viewModelScope.launch {
                val newConv = repository.createConversation(userId)
                selectConversation(newConv.id)
                processSendingMessage(newConv.id, trimmed, userId)
            }
        } else {
            viewModelScope.launch {
                processSendingMessage(convId, trimmed, userId)
            }
        }
    }

    private suspend fun processSendingMessage(convId: String, text: String, userId: String) {
        _isGenerating.value = true
        try {
            val response = repository.sendMessage(convId, text, userId)
            if (response.safetyRisk == "crisis") {
                _showCrisisModal.value = true
            }
        } catch (e: Exception) {
            _snackBarMessage.value = "Error generating response: ${e.message}"
        } finally {
            _isGenerating.value = false
        }
    }

    fun toggleCrisisModal(show: Boolean) {
        _showCrisisModal.value = show
    }

    fun toggleBreathingModal(show: Boolean) {
        _showBreathingModal.value = show
    }

    fun recordMood(mood: String, score: Int, note: String) {
        viewModelScope.launch {
            val userId = currentUser.value?.id ?: "guest_user"
            repository.recordMood(userId, mood, score, note)
            _snackBarMessage.value = "Mood entry saved: $mood"
        }
    }

    // CSV File upload & parsing
    fun uploadCsvFile(uri: Uri) {
        viewModelScope.launch {
            _datasetUiState.value = _datasetUiState.value.copy(isUploading = true, uploadError = null)
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _datasetUiState.value = _datasetUiState.value.copy(
                        isUploading = false,
                        uploadError = "Could not open selected file."
                    )
                    return@launch
                }

                val parseResult = repository.importCsvStream(inputStream, "uploaded_dataset.csv")
                _datasetUiState.value = _datasetUiState.value.copy(
                    isUploading = false,
                    lastImportStats = parseResult,
                    uploadError = if (parseResult.validRows == 0) "No valid rows found in CSV." else null
                )
                _snackBarMessage.value = "Successfully imported ${parseResult.validRows} dataset items!"
            } catch (e: Exception) {
                _datasetUiState.value = _datasetUiState.value.copy(
                    isUploading = false,
                    uploadError = "CSV Import Failed: ${e.message}"
                )
            }
        }
    }

    fun uploadCsvContent(content: String) {
        viewModelScope.launch {
            _datasetUiState.value = _datasetUiState.value.copy(isUploading = true, uploadError = null)
            try {
                val parseResult = repository.importCsvText(content, "pasted_dataset.csv")
                _datasetUiState.value = _datasetUiState.value.copy(
                    isUploading = false,
                    lastImportStats = parseResult,
                    uploadError = if (parseResult.validRows == 0) "No valid rows parsed." else null
                )
                _snackBarMessage.value = "Successfully imported ${parseResult.validRows} dataset items!"
            } catch (e: Exception) {
                _datasetUiState.value = _datasetUiState.value.copy(
                    isUploading = false,
                    uploadError = "Import Failed: ${e.message}"
                )
            }
        }
    }

    fun clearDataset() {
        viewModelScope.launch {
            repository.clearDataset()
            _snackBarMessage.value = "Dataset cleared."
        }
    }

    fun setDatasetSearchQuery(query: String) {
        _datasetUiState.value = _datasetUiState.value.copy(searchQuery = query)
    }

    fun setDatasetCategoryFilter(category: String) {
        _datasetUiState.value = _datasetUiState.value.copy(filterCategory = category)
    }

    // Realtime sync from Supabase
    fun triggerRealtimeSync() {
        viewModelScope.launch {
            _datasetUiState.value = _datasetUiState.value.copy(isRealtimeSyncing = true)
            val result = repository.syncFromRemoteSupabase()
            result.onSuccess { count ->
                _datasetUiState.value = _datasetUiState.value.copy(
                    isRealtimeSyncing = false,
                    syncStatusMessage = "Synced $count items from Supabase Realtime."
                )
                _snackBarMessage.value = "Supabase sync completed ($count items)."
            }.onFailure { err ->
                _datasetUiState.value = _datasetUiState.value.copy(
                    isRealtimeSyncing = false,
                    syncStatusMessage = "Sync notice: ${err.message}"
                )
            }
        }
    }

    private fun startPeriodicRealtimeSync() {
        viewModelScope.launch {
            while (true) {
                delay(30_000) // check every 30s
                if (supabaseClient.isConfigured) {
                    repository.syncFromRemoteSupabase()
                }
            }
        }
    }

    // Supabase Auth
    fun signUp(email: String, pass: String, name: String) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        if (cleanEmail.isEmpty() || cleanPass.length < 6) {
            _authError.value = "Please provide a valid email and minimum 6-character password."
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            _authSuccess.value = null
            val result = repository.signUp(cleanEmail, cleanPass, name.trim())
            _isAuthLoading.value = false
            result.onSuccess {
                _authSuccess.value = "Account created successfully! Welcome to MindCare AI."
                _snackBarMessage.value = "Signed in as ${it.name}"
            }.onFailure { err ->
                _authError.value = err.message ?: "Sign up failed."
            }
        }
    }

    fun signIn(email: String, pass: String) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        if (cleanEmail.isEmpty() || cleanPass.isEmpty()) {
            _authError.value = "Email and password are required."
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            _authSuccess.value = null
            val result = repository.signIn(cleanEmail, cleanPass)
            _isAuthLoading.value = false
            result.onSuccess {
                _authSuccess.value = "Welcome back, ${it.name}!"
                _snackBarMessage.value = "Signed in successfully"
                _currentTab.value = ScreenTab.CHAT
            }.onFailure { err ->
                _authError.value = err.message ?: "Login failed."
            }
        }
    }

    fun resetPassword(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            _authError.value = "Please enter your email to receive recovery instructions."
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = repository.resetPassword(cleanEmail)
            _isAuthLoading.value = false
            result.onSuccess { msg ->
                _authSuccess.value = msg
            }.onFailure { err ->
                _authError.value = err.message ?: "Could not request password reset."
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _snackBarMessage.value = "Signed out."
        }
    }

    fun configureSupabase(url: String, anonKey: String) {
        supabaseClient.supabaseUrl = url
        supabaseClient.supabaseAnonKey = anonKey
        _snackBarMessage.value = "Supabase configuration updated."
    }

    fun exportDataset() {
        viewModelScope.launch {
            val csv = repository.exportDataset()
            _snackBarMessage.value = "Dataset exported (${datasetItems.value.size} records ready)."
        }
    }

    fun clearSnackBar() {
        _snackBarMessage.value = null
    }
}
