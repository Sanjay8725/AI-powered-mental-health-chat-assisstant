package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BreathingExerciseDialog
import com.example.ui.components.CrisisEscalationDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DatasetManagerScreen
import com.example.ui.screens.KnowledgeRAGScreen
import com.example.ui.screens.MoodTrackerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MindCareViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {
    private val viewModel: MindCareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MindCareApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MindCareApp(viewModel: MindCareViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentConversationId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

    val showCrisisModal by viewModel.showCrisisModal.collectAsStateWithLifecycle()
    val showBreathingModal by viewModel.showBreathingModal.collectAsStateWithLifecycle()

    val moods by viewModel.moods.collectAsStateWithLifecycle()
    val knowledgeResources by viewModel.knowledgeResources.collectAsStateWithLifecycle()
    val datasetItems by viewModel.datasetItems.collectAsStateWithLifecycle()
    val datasetCount by viewModel.datasetCount.collectAsStateWithLifecycle()
    val datasetUiState by viewModel.datasetUiState.collectAsStateWithLifecycle()

    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val authSuccess by viewModel.authSuccess.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val snackBarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackBarMessage) {
        snackBarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackBar()
        }
    }

    // Handle back button on sub-screens
    if (currentTab != ScreenTab.CHAT) {
        BackHandler {
            viewModel.setTab(ScreenTab.CHAT)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("main_bottom_nav")) {
                NavigationBarItem(
                    selected = currentTab == ScreenTab.CHAT,
                    onClick = { viewModel.setTab(ScreenTab.CHAT) },
                    icon = {
                        Icon(
                            if (currentTab == ScreenTab.CHAT) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = "Chat"
                        )
                    },
                    label = { Text("Chat") },
                    modifier = Modifier.testTag("nav_item_chat")
                )

                NavigationBarItem(
                    selected = currentTab == ScreenTab.MOOD,
                    onClick = { viewModel.setTab(ScreenTab.MOOD) },
                    icon = {
                        Icon(
                            if (currentTab == ScreenTab.MOOD) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Mood"
                        )
                    },
                    label = { Text("Mood") },
                    modifier = Modifier.testTag("nav_item_mood")
                )

                NavigationBarItem(
                    selected = currentTab == ScreenTab.KNOWLEDGE,
                    onClick = { viewModel.setTab(ScreenTab.KNOWLEDGE) },
                    icon = {
                        Icon(
                            if (currentTab == ScreenTab.KNOWLEDGE) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = "RAG Guides"
                        )
                    },
                    label = { Text("Guides") },
                    modifier = Modifier.testTag("nav_item_knowledge")
                )

                NavigationBarItem(
                    selected = currentTab == ScreenTab.DATASET,
                    onClick = { viewModel.setTab(ScreenTab.DATASET) },
                    icon = {
                        Icon(
                            if (currentTab == ScreenTab.DATASET) Icons.Default.Storage else Icons.Outlined.Storage,
                            contentDescription = "Dataset"
                        )
                    },
                    label = { Text("Dataset") },
                    modifier = Modifier.testTag("nav_item_dataset")
                )

                NavigationBarItem(
                    selected = currentTab == ScreenTab.AUTH,
                    onClick = { viewModel.setTab(ScreenTab.AUTH) },
                    icon = {
                        Icon(
                            if (currentTab == ScreenTab.AUTH) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = "Account"
                        )
                    },
                    label = { Text("Account") },
                    modifier = Modifier.testTag("nav_item_auth")
                )
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.CHAT -> ChatScreen(
                    conversations = conversations,
                    currentConversationId = currentConversationId,
                    messages = messages,
                    isGenerating = isGenerating,
                    onSendMessage = { viewModel.sendMessage(it) },
                    onSelectConversation = { viewModel.selectConversation(it) },
                    onNewConversation = { viewModel.startNewConversation() },
                    onDeleteConversation = { viewModel.deleteConversation(it) },
                    onOpenCrisis = { viewModel.toggleCrisisModal(true) },
                    onOpenBreathing = { viewModel.toggleBreathingModal(true) }
                )
                ScreenTab.MOOD -> MoodTrackerScreen(
                    moods = moods,
                    onSaveMood = { mood, score, note ->
                        viewModel.recordMood(mood, score, note)
                    }
                )
                ScreenTab.KNOWLEDGE -> KnowledgeRAGScreen(
                    resources = knowledgeResources,
                    onDiscussInChat = { prompt ->
                        viewModel.setTab(ScreenTab.CHAT)
                        viewModel.sendMessage(prompt)
                    }
                )
                ScreenTab.DATASET -> DatasetManagerScreen(
                    datasetItems = datasetItems,
                    datasetCount = datasetCount,
                    datasetUiState = datasetUiState,
                    isSupabaseConfigured = viewModel.supabaseClient.isConfigured,
                    onUploadCsvFile = { viewModel.uploadCsvFile(it) },
                    onUploadCsvContent = { viewModel.uploadCsvContent(it) },
                    onClearDataset = { viewModel.clearDataset() },
                    onExportDataset = { viewModel.exportDataset() },
                    onTriggerRealtimeSync = { viewModel.triggerRealtimeSync() },
                    onSearchChange = { viewModel.setDatasetSearchQuery(it) },
                    onCategoryFilterChange = { viewModel.setDatasetCategoryFilter(it) }
                )
                ScreenTab.AUTH -> AuthScreen(
                    currentUser = currentUser,
                    supabaseClient = viewModel.supabaseClient,
                    isLoading = isAuthLoading,
                    errorMessage = authError,
                    successMessage = authSuccess,
                    onSignIn = { email, pass -> viewModel.signIn(email, pass) },
                    onSignUp = { email, pass, name -> viewModel.signUp(email, pass, name) },
                    onResetPassword = { email -> viewModel.resetPassword(email) },
                    onSignOut = { viewModel.signOut() },
                    onSaveSupabaseConfig = { url, key -> viewModel.configureSupabase(url, key) }
                )
            }

            // Dialogs
            if (showCrisisModal) {
                CrisisEscalationDialog(
                    onDismiss = { viewModel.toggleCrisisModal(false) },
                    onOpenBreathing = {
                        viewModel.toggleCrisisModal(false)
                        viewModel.toggleBreathingModal(true)
                    }
                )
            }

            if (showBreathingModal) {
                BreathingExerciseDialog(
                    onDismiss = { viewModel.toggleBreathingModal(false) }
                )
            }
        }
    }
}
