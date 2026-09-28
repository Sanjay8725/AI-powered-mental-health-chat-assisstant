package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.entity.DatasetItemEntity
import com.example.data.local.entity.MoodEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class SupabaseClient(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mindcare_supabase_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    var supabaseUrl: String
        get() = prefs.getString("supabase_url", "https://xyzcompany.supabase.co") ?: "https://xyzcompany.supabase.co"
        set(value) = prefs.edit().putString("supabase_url", value.trim().removeSuffix("/")).apply()

    var supabaseAnonKey: String
        get() = prefs.getString("supabase_anon_key", "") ?: ""
        set(value) = prefs.edit().putString("supabase_anon_key", value.trim()).apply()

    var currentAuthToken: String?
        get() = prefs.getString("supabase_jwt_token", null)
        set(value) = prefs.edit().putString("supabase_jwt_token", value).apply()

    var isConfigured: Boolean
        get() = supabaseUrl.isNotEmpty() && supabaseAnonKey.isNotEmpty() && !supabaseUrl.contains("xyzcompany")
        set(_) {}

    // 1. Supabase Auth: Register
    suspend fun signUp(email: String, pass: String, name: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            // Local offline fallback user
            val localUser = UserEntity(
                id = UUID.randomUUID().toString(),
                email = email,
                name = name.ifBlank { email.substringBefore("@") },
                token = "local_token_${System.currentTimeMillis()}",
                isSupabaseUser = false
            )
            return@withContext Result.success(localUser)
        }

        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("data", JSONObject().apply {
                    put("display_name", name)
                })
            }

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/signup")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errObj = try { JSONObject(body) } catch (_: Exception) { null }
                val errorMsg = errObj?.optString("error_description")
                    ?: errObj?.optString("msg")
                    ?: "Signup failed (${response.code})"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val resObj = JSONObject(body)
            val accessToken = resObj.optString("access_token", "")
            if (accessToken.isNotEmpty()) {
                currentAuthToken = accessToken
            }

            val userObj = resObj.optJSONObject("user") ?: resObj
            val userId = userObj.optString("id", UUID.randomUUID().toString())
            val userEmail = userObj.optString("email", email)

            val user = UserEntity(
                id = userId,
                email = userEmail,
                name = name.ifBlank { userEmail.substringBefore("@") },
                token = accessToken.ifEmpty { "session_${System.currentTimeMillis()}" },
                isSupabaseUser = true
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Supabase Auth: Login
    suspend fun signIn(email: String, pass: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            // Local offline mock login
            val localUser = UserEntity(
                id = "offline_user_1",
                email = email,
                name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                token = "local_offline_jwt",
                isSupabaseUser = false
            )
            return@withContext Result.success(localUser)
        }

        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errObj = try { JSONObject(body) } catch (_: Exception) { null }
                val errorMsg = errObj?.optString("error_description")
                    ?: errObj?.optString("msg")
                    ?: "Login failed (${response.code})"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val resObj = JSONObject(body)
            val accessToken = resObj.optString("access_token")
            currentAuthToken = accessToken

            val userObj = resObj.optJSONObject("user")
            val userId = userObj?.optString("id") ?: UUID.randomUUID().toString()
            val userEmail = userObj?.optString("email", email) ?: email
            val metadata = userObj?.optJSONObject("user_metadata")
            val name = metadata?.optString("display_name", "")?.takeIf { it.isNotBlank() }
                ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

            val user = UserEntity(
                id = userId,
                email = userEmail,
                name = name,
                token = accessToken,
                isSupabaseUser = true
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. Supabase Auth: Password Reset
    suspend fun resetPassword(email: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.success("Password recovery email sent (Simulated mode).")
        }

        try {
            val json = JSONObject().apply {
                put("email", email)
            }

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/recover")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Password reset instructions have been sent to $email.")
            } else {
                Result.failure(Exception("Could not initiate password reset (${response.code})."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. Supabase Auth: Logout
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        val token = currentAuthToken
        currentAuthToken = null
        if (isConfigured && token != null) {
            try {
                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/logout")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $token")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute()
            } catch (_: Exception) {
                // best effort
            }
        }
        Result.success(Unit)
    }

    // 5. PostgREST: Push dataset items to Supabase
    suspend fun syncDatasetToSupabase(items: List<DatasetItemEntity>): Result<Int> = withContext(Dispatchers.IO) {
        if (!isConfigured || items.isEmpty()) {
            return@withContext Result.success(items.size)
        }

        try {
            val array = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("prompt", item.prompt)
                    put("response", item.response)
                    put("category", item.category)
                    put("emotion", item.emotion)
                    put("safety_level", item.safetyLevel)
                    put("source", item.source)
                    put("imported_at", item.importedAt)
                }
                array.put(obj)
            }

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/dataset_items")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${currentAuthToken ?: supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(array.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(items.size)
            } else {
                Result.failure(Exception("Supabase sync returned ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. PostgREST: Pull dataset items from Supabase (Realtime / Remote Sync)
    suspend fun fetchRemoteDataset(): Result<List<DatasetItemEntity>> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.success(emptyList())
        }

        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/dataset_items?select=*&order=imported_at.desc&limit=200")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${currentAuthToken ?: supabaseAnonKey}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Supabase fetch failed: ${response.code}"))
            }

            val array = JSONArray(body)
            val list = mutableListOf<DatasetItemEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DatasetItemEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        prompt = obj.optString("prompt", ""),
                        response = obj.optString("response", ""),
                        category = obj.optString("category", "wellness"),
                        emotion = obj.optString("emotion", "neutral"),
                        safetyLevel = obj.optString("safety_level", "safe"),
                        source = obj.optString("source", "supabase_remote"),
                        importedAt = obj.optLong("imported_at", System.currentTimeMillis())
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 7. PostgREST: Push individual mood entry to Supabase database with timestamp
    suspend fun syncMoodToSupabase(mood: MoodEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.success(Unit) // Offline/Local fallback
        }

        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val isoDate = isoFormat.format(Date(mood.createdAt))

            val obj = JSONObject().apply {
                put("id", mood.id)
                put("user_id", mood.userId)
                put("mood", mood.mood)
                put("score", mood.score)
                put("note", mood.note)
                put("created_at", isoDate)
                put("timestamp", mood.createdAt)
            }

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/moods")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${currentAuthToken ?: supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(obj.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Supabase mood sync returned ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 8. PostgREST: Fetch user's mood entries from Supabase
    suspend fun fetchRemoteMoods(userId: String): Result<List<MoodEntity>> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.success(emptyList())
        }

        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/moods?user_id=eq.$userId&order=timestamp.desc&limit=50")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${currentAuthToken ?: supabaseAnonKey}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Supabase mood fetch failed: ${response.code}"))
            }

            val array = JSONArray(body)
            val list = mutableListOf<MoodEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MoodEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("user_id", userId),
                        mood = obj.optString("mood", "Calm"),
                        score = obj.optInt("score", 3),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
