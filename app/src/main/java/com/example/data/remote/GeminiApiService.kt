package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.KnowledgeResourceEntity
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiResponse(
    val text: String,
    val detectedEmotion: String, // joy, sadness, anxiety, stress, anger, neutral
    val safetyRisk: String, // safe, moderate_risk, crisis
    val groundingCitations: List<String> = emptyList()
)

class GeminiApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val model = "gemini-3.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    suspend fun generateSupportiveResponse(
        userMessage: String,
        recentHistory: List<MessageEntity>,
        retrievedContext: List<KnowledgeResourceEntity>
    ): AiResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Local crisis pre-check
        val safetyRisk = detectSafetyRisk(userMessage)
        val emotion = detectEmotion(userMessage)

        if (safetyRisk == "crisis") {
            return@withContext AiResponse(
                text = "I hear how much pain you're in, and your life and safety matter deeply. Because I am an AI supportive wellness companion and not a crisis counselor or doctor, I cannot provide emergency crisis intervention. Please connect right away with professionals who can support you through this moment safely:\n\n• National Suicide & Crisis Lifeline: Call or text 988 (Available 24/7, free and confidential)\n• Crisis Text Line: Text HOME to 741741\n• Emergency Services: Call 911 (or your local emergency number)\n• International Resources: Visit https://findahelpline.com\n\nYou do not have to carry this alone. Would you like to pause and try a gentle 5-4-3-2-1 grounding exercise with me while reaching out?",
                detectedEmotion = "stress",
                safetyRisk = "crisis"
            )
        }

        // If API key is not configured or placeholder, provide grounded local response
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val localResponse = generateLocalGroundedResponse(userMessage, retrievedContext, emotion)
            return@withContext AiResponse(
                text = localResponse,
                detectedEmotion = emotion,
                safetyRisk = safetyRisk,
                groundingCitations = retrievedContext.map { it.title }.take(2)
            )
        }

        try {
            // Build RAG Context text
            val ragPassages = retrievedContext.joinToString("\n\n") { resource ->
                "Source [${resource.source} - ${resource.title}]:\n${resource.content}"
            }

            val systemPolicy = """
                You are MindCare AI, a supportive, empathetic, non-diagnostic mental wellness conversational assistant.
                
                IMPORTANT SCOPE & SAFETY BOUNDARIES:
                1. You are NOT a doctor, psychologist, or licensed therapist. Never claim to diagnose medical/mental conditions or prescribe treatments/medications.
                2. If the user expresses imminent crisis, suicide, or severe self-harm, prioritize safety immediately.
                3. Be warm, validating, calm, concise, and non-judgmental. Keep responses digestible and encouraging.
                4. Ground your psychoeducational techniques in the retrieved knowledge base below when appropriate.
                5. Encourage healthy coping mechanisms (mindful breathing, cognitive reframing, grounding, sleep hygiene, gentle movement).
                
                KNOWLEDGE BASE PASSAGES (RAG CONTEXT):
                $ragPassages
                
                FORMATTING:
                Provide a compassionate answer. At the very end of your response, on a new line, output a JSON tag formatted exactly like:
                <!--METADATA: {"emotion": "anxiety|sadness|joy|stress|anger|neutral", "safety": "safe|moderate_risk"}-->
            """.trimIndent()

            val contentsArray = JSONArray()

            // System instruction
            val systemPart = JSONObject().apply { put("text", systemPolicy) }
            val systemObj = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(systemPart))
            }
            contentsArray.put(systemObj)

            val modelAck = JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", "Understood. I will act as MindCare AI, providing warm, non-diagnostic wellness support while observing strict safety boundaries and RAG context.")
                }))
            }
            contentsArray.put(modelAck)

            // Recent history
            recentHistory.takeLast(6).forEach { msg ->
                val role = if (msg.role == "assistant") "model" else "user"
                val obj = JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().apply { put("text", msg.content) }))
                }
                contentsArray.put(obj)
            }

            // Current user message
            val currentMessageObj = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().apply { put("text", userMessage) }))
            }
            contentsArray.put(currentMessageObj)

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.9)
                    put("maxOutputTokens", 800)
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w("MindCareAI", "Gemini API error: ${response.code} $responseBody")
                val localFallback = generateLocalGroundedResponse(userMessage, retrievedContext, emotion)
                return@withContext AiResponse(
                    text = localFallback,
                    detectedEmotion = emotion,
                    safetyRisk = safetyRisk
                )
            }

            val resObj = JSONObject(responseBody)
            val candidates = resObj.optJSONArray("candidates")
            val rawText = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "") ?: ""

            // Parse metadata tag if present
            var finalEmotion = emotion
            var finalRisk = safetyRisk
            var cleanText = rawText

            val metaRegex = Regex("""<!--METADATA:\s*(\{.*?\})\s*-->""", RegexOption.DOT_MATCHES_ALL)
            val match = metaRegex.find(rawText)
            if (match != null) {
                cleanText = rawText.replace(match.value, "").trim()
                try {
                    val metaObj = JSONObject(match.groupValues[1])
                    val metaEmotion = metaObj.optString("emotion")
                    if (metaEmotion.isNotBlank()) finalEmotion = metaEmotion
                    val metaSafety = metaObj.optString("safety")
                    if (metaSafety == "crisis") finalRisk = "crisis"
                } catch (_: Exception) {}
            }

            if (cleanText.isBlank()) {
                cleanText = generateLocalGroundedResponse(userMessage, retrievedContext, emotion)
            }

            AiResponse(
                text = cleanText,
                detectedEmotion = finalEmotion,
                safetyRisk = finalRisk,
                groundingCitations = retrievedContext.map { it.title }.take(2)
            )
        } catch (e: Exception) {
            Log.e("MindCareAI", "API exception", e)
            val localFallback = generateLocalGroundedResponse(userMessage, retrievedContext, emotion)
            AiResponse(
                text = localFallback,
                detectedEmotion = emotion,
                safetyRisk = safetyRisk
            )
        }
    }

    fun detectSafetyRisk(text: String): String {
        val lower = text.lowercase()
        val crisisKeywords = listOf(
            "kill myself", "want to die", "commit suicide", "end my life",
            "hang myself", "suicidal", "better off dead", "cutting myself",
            "self harm", "overdose", "slit my", "can't go on living",
            "no reason to live", "take my own life"
        )
        if (crisisKeywords.any { lower.contains(it) }) {
            return "crisis"
        }
        val moderateKeywords = listOf(
            "hopeless", "worthless", "can't take it anymore", "overwhelmed",
            "panic attack", "nobody cares", "deep depression", "giving up"
        )
        if (moderateKeywords.any { lower.contains(it) }) {
            return "moderate_risk"
        }
        return "safe"
    }

    fun detectEmotion(text: String): String {
        val lower = text.lowercase()
        return when {
            listOf("anxious", "panic", "worry", "nervous", "scared", "dread", "shaking", "fear").any { lower.contains(it) } -> "anxiety"
            listOf("sad", "crying", "depressed", "lonely", "grief", "empty", "heartbroken", "down").any { lower.contains(it) } -> "sadness"
            listOf("stressed", "burned out", "pressure", "exhausted", "too much work", "deadline", "busy").any { lower.contains(it) } -> "stress"
            listOf("angry", "furious", "mad", "frustrated", "irritated", "pissed", "annoyed").any { lower.contains(it) } -> "anger"
            listOf("happy", "grateful", "good", "great", "relieved", "peaceful", "proud", "joy").any { lower.contains(it) } -> "joy"
            else -> "neutral"
        }
    }

    private fun generateLocalGroundedResponse(
        prompt: String,
        context: List<KnowledgeResourceEntity>,
        emotion: String
    ): String {
        val topKnowledge = context.firstOrNull()
        val knowledgeSnippet = topKnowledge?.content?.take(280)?.let {
            "\n\n💡 *Curated Coping Practice (${topKnowledge.title})*:\n$it..."
        } ?: ""

        return when (emotion) {
            "anxiety" ->
                "I hear that you're feeling anxious right now, and that can feel so overwhelming in the body. Take a gentle breath with me. Let's try the 5-4-3-2-1 grounding exercise: notice 5 things you can see around you, 4 things you can physically touch, 3 things you can hear, 2 things you can smell, and 1 slow breath.$knowledgeSnippet\n\nHow does your chest or shoulders feel right now?"
            "stress" ->
                "It sounds like you're carrying a heavy weight on your shoulders today. Remember that taking even a two-minute pause to breathe and reset is not lost time; it protects your energy. What is one small, manageable task you can set aside for tomorrow?$knowledgeSnippet"
            "sadness" ->
                "Thank you for sharing this with me. It is completely okay to feel sad, and you don't have to force yourself to be okay right this second. Give yourself permission to be gentle with yourself. Is there a warm tea, comfortable blanket, or gentle music that could bring you comfort today?$knowledgeSnippet"
            "anger" ->
                "Your frustration is valid. When anger surges, our nervous system enters a fight-or-flight mode. Try placing one hand on your chest and taking three prolonged exhalations, releasing the physical tension in your jaw and hands.$knowledgeSnippet"
            "joy" ->
                "I love hearing that! Savoring positive moments, no matter how small, strengthens mental resilience and mood balance. What was the highlight of this moment for you?"
            else ->
                "Thank you for reaching out. I'm here to listen, support, and explore helpful coping techniques with you in a safe, non-judgmental space. Would you like to talk through what's on your mind or try a quick calming exercise?$knowledgeSnippet"
        }
    }
}
