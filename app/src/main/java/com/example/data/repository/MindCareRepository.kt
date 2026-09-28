package com.example.data.repository

import com.example.data.local.dao.AppDao
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.DatasetItemEntity
import com.example.data.local.entity.KnowledgeResourceEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MoodEntity
import com.example.data.local.entity.SafetyEventEntity
import com.example.data.local.entity.UserEntity
import com.example.data.remote.AiResponse
import com.example.data.remote.GeminiApiService
import com.example.data.remote.SupabaseClient
import com.example.data.util.CsvDatasetParser
import com.example.data.util.CsvParseResult
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.util.UUID

class MindCareRepository(
    private val appDao: AppDao,
    val supabaseClient: SupabaseClient,
    private val geminiService: GeminiApiService
) {
    val currentUserFlow: Flow<UserEntity?> = appDao.getCurrentUserFlow()
    val knowledgeFlow: Flow<List<KnowledgeResourceEntity>> = appDao.getKnowledgeResourcesFlow()
    val datasetItemsFlow: Flow<List<DatasetItemEntity>> = appDao.getDatasetItemsFlow()
    val datasetCountFlow: Flow<Int> = appDao.getDatasetItemCountFlow()
    val safetyEventsFlow: Flow<List<SafetyEventEntity>> = appDao.getSafetyEventsFlow()

    fun getConversationsFlow(userId: String): Flow<List<ConversationEntity>> =
        appDao.getConversationsFlow(userId)

    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>> =
        appDao.getMessagesFlow(conversationId)

    fun getMoodsFlow(userId: String): Flow<List<MoodEntity>> =
        appDao.getMoodsFlow(userId)

    suspend fun initializeSeeds() {
        val existingKnowledge = appDao.getAllKnowledgeSync()
        if (existingKnowledge.isEmpty()) {
            appDao.insertKnowledgeResources(CsvDatasetParser.getInitialKnowledgeResources())
        }

        val existingDatasetCount = appDao.getDatasetItemCount()
        if (existingDatasetCount == 0) {
            appDao.insertDatasetItems(CsvDatasetParser.getInitialDatasetItems())
        }

        // Create default guest user if no user exists
        val currentUser = appDao.getCurrentUser()
        if (currentUser == null) {
            val guest = UserEntity(
                id = "guest_user",
                email = "guest@mindcare.ai",
                name = "Mindful Explorer",
                token = null,
                isSupabaseUser = false
            )
            appDao.insertUser(guest)
        }
    }

    suspend fun signUp(email: String, pass: String, name: String): Result<UserEntity> {
        val result = supabaseClient.signUp(email, pass, name)
        result.onSuccess { user ->
            appDao.clearUser()
            appDao.insertUser(user)
        }
        return result
    }

    suspend fun signIn(email: String, pass: String): Result<UserEntity> {
        val result = supabaseClient.signIn(email, pass)
        result.onSuccess { user ->
            appDao.clearUser()
            appDao.insertUser(user)
        }
        return result
    }

    suspend fun resetPassword(email: String): Result<String> {
        return supabaseClient.resetPassword(email)
    }

    suspend fun signOut() {
        supabaseClient.signOut()
        appDao.clearUser()
        val guest = UserEntity(
            id = "guest_user",
            email = "guest@mindcare.ai",
            name = "Mindful Explorer",
            token = null,
            isSupabaseUser = false
        )
        appDao.insertUser(guest)
    }

    suspend fun createConversation(userId: String, title: String = "Mindful Check-in"): ConversationEntity {
        val conv = ConversationEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        appDao.insertConversation(conv)

        // Seed initial welcoming message
        val welcomeMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conv.id,
            role = "assistant",
            content = "Hello, I am MindCare AI, your supportive mental health and wellness companion. I'm here to listen, offer calming perspectives, and explore evidence-based coping strategies with you. How are you feeling today?",
            timestamp = System.currentTimeMillis(),
            emotion = "neutral",
            safetyLabel = "safe"
        )
        appDao.insertMessage(welcomeMsg)
        return conv
    }

    suspend fun deleteConversation(id: String) {
        appDao.deleteMessagesByConversation(id)
        appDao.deleteConversation(id)
    }

    suspend fun sendMessage(
        conversationId: String,
        userText: String,
        userId: String
    ): AiResponse {
        val userMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "user",
            content = userText,
            timestamp = System.currentTimeMillis(),
            emotion = geminiService.detectEmotion(userText),
            safetyLabel = geminiService.detectSafetyRisk(userText)
        )
        appDao.insertMessage(userMsg)

        // Update conversation timestamp & title if first message
        val conversation = appDao.getConversationById(conversationId)
        if (conversation != null) {
            val isFirstUserMsg = (appDao.getMessages(conversationId).size <= 2)
            val updatedTitle = if (isFirstUserMsg) {
                userText.take(32).trim() + if (userText.length > 32) "..." else ""
            } else conversation.title
            appDao.updateConversation(conversation.copy(title = updatedTitle, updatedAt = System.currentTimeMillis()))
        }

        // RAG Retrieval: search local knowledge base and dataset
        val queryKeywords = userText.split(" ").filter { it.length > 3 }.take(4)
        val retrieved = mutableListOf<KnowledgeResourceEntity>()
        for (kw in queryKeywords) {
            val matches = appDao.searchKnowledge(kw)
            retrieved.addAll(matches)
        }
        if (retrieved.isEmpty()) {
            retrieved.addAll(appDao.getAllKnowledgeSync().take(2))
        }
        val distinctContext = retrieved.distinctBy { it.id }.take(3)

        // History
        val recentHistory = appDao.getMessages(conversationId)

        // Call AI Service
        val aiResponse = geminiService.generateSupportiveResponse(userText, recentHistory, distinctContext)

        val isCrisis = aiResponse.safetyRisk == "crisis"
        val assistantMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "assistant",
            content = aiResponse.text,
            timestamp = System.currentTimeMillis(),
            emotion = aiResponse.detectedEmotion,
            safetyLabel = aiResponse.safetyRisk,
            isSafetyEscalation = isCrisis
        )
        appDao.insertMessage(assistantMsg)

        // If crisis or moderate risk, record safety event for audit
        if (aiResponse.safetyRisk != "safe") {
            val event = SafetyEventEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                conversationId = conversationId,
                riskLevel = aiResponse.safetyRisk,
                triggerPhrase = userText.take(120),
                actionTaken = if (isCrisis) "escalated_crisis_lifeline" else "provided_grounding_support"
            )
            appDao.insertSafetyEvent(event)
        }

        return aiResponse
    }

    suspend fun recordMood(userId: String, mood: String, score: Int, note: String): MoodEntity {
        val moodEntity = MoodEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            mood = mood,
            score = score,
            note = note,
            createdAt = System.currentTimeMillis()
        )
        appDao.insertMood(moodEntity)

        // Save to Supabase database with timestamp
        if (supabaseClient.isConfigured) {
            supabaseClient.syncMoodToSupabase(moodEntity)
        }
        return moodEntity
    }

    suspend fun syncMoodsFromRemote(userId: String): Result<Int> {
        val result = supabaseClient.fetchRemoteMoods(userId)
        return result.map { list ->
            for (m in list) {
                appDao.insertMood(m)
            }
            list.size
        }
    }

    suspend fun importCsvStream(inputStream: InputStream, filename: String): CsvParseResult {
        val parseResult = CsvDatasetParser.parseCsv(inputStream, filename)
        if (parseResult.items.isNotEmpty()) {
            appDao.insertDatasetItems(parseResult.items)
            // Sync to Supabase in background if configured
            if (supabaseClient.isConfigured) {
                supabaseClient.syncDatasetToSupabase(parseResult.items)
            }
        }
        return parseResult
    }

    suspend fun importCsvText(csvContent: String, sourceName: String): CsvParseResult {
        val parseResult = CsvDatasetParser.parseCsvText(csvContent, sourceName)
        if (parseResult.items.isNotEmpty()) {
            appDao.insertDatasetItems(parseResult.items)
            if (supabaseClient.isConfigured) {
                supabaseClient.syncDatasetToSupabase(parseResult.items)
            }
        }
        return parseResult
    }

    suspend fun clearDataset() {
        appDao.clearDataset()
    }

    suspend fun exportDataset(): String {
        val items = appDao.getAllDatasetItemsSync()
        return CsvDatasetParser.exportDatasetToCsv(items)
    }

    suspend fun syncFromRemoteSupabase(): Result<Int> {
        val remoteResult = supabaseClient.fetchRemoteDataset()
        return remoteResult.map { remoteItems ->
            if (remoteItems.isNotEmpty()) {
                appDao.insertDatasetItems(remoteItems)
            }
            remoteItems.size
        }
    }
}
