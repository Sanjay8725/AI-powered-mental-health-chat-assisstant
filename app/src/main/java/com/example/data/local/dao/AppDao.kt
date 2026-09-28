package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.DatasetItemEntity
import com.example.data.local.entity.KnowledgeResourceEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MoodEntity
import com.example.data.local.entity.SafetyEventEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // User
    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUser()

    // Conversations
    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getConversationsFlow(userId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    // Messages
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessages(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)

    // Moods
    @Query("SELECT * FROM moods WHERE userId = :userId ORDER BY createdAt DESC")
    fun getMoodsFlow(userId: String): Flow<List<MoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntity)

    @Query("DELETE FROM moods WHERE id = :id")
    suspend fun deleteMood(id: String)

    // Knowledge Resources (RAG)
    @Query("SELECT * FROM knowledge_resources ORDER BY category ASC")
    fun getKnowledgeResourcesFlow(): Flow<List<KnowledgeResourceEntity>>

    @Query("SELECT * FROM knowledge_resources WHERE category = :category ORDER BY title ASC")
    fun getKnowledgeResourcesByCategory(category: String): Flow<List<KnowledgeResourceEntity>>

    @Query("SELECT * FROM knowledge_resources WHERE content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    suspend fun searchKnowledge(query: String): List<KnowledgeResourceEntity>

    @Query("SELECT * FROM knowledge_resources")
    suspend fun getAllKnowledgeSync(): List<KnowledgeResourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgeResources(resources: List<KnowledgeResourceEntity>)

    // Dataset Items (CSV / Ingested Dataset)
    @Query("SELECT * FROM dataset_items ORDER BY importedAt DESC")
    fun getDatasetItemsFlow(): Flow<List<DatasetItemEntity>>

    @Query("SELECT COUNT(*) FROM dataset_items")
    fun getDatasetItemCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dataset_items")
    suspend fun getDatasetItemCount(): Int

    @Query("SELECT * FROM dataset_items WHERE prompt LIKE '%' || :query || '%' OR response LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    suspend fun searchDataset(query: String): List<DatasetItemEntity>

    @Query("SELECT * FROM dataset_items ORDER BY importedAt DESC LIMIT 200")
    suspend fun getAllDatasetItemsSync(): List<DatasetItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDatasetItems(items: List<DatasetItemEntity>)

    @Query("DELETE FROM dataset_items WHERE source = :source")
    suspend fun deleteDatasetBySource(source: String)

    @Query("DELETE FROM dataset_items")
    suspend fun clearDataset()

    // Safety Events
    @Query("SELECT * FROM safety_events ORDER BY createdAt DESC")
    fun getSafetyEventsFlow(): Flow<List<SafetyEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafetyEvent(event: SafetyEventEntity)
}
