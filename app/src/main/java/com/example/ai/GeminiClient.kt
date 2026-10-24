package com.example.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(val text: String) : GeminiResult()
    data class Failure(val bengaliMessage: String, val technicalDetail: String, val isConfigError: Boolean = false) : GeminiResult()
}

class GeminiClient(private val context: Context) {
    companion object {
        private const val TAG = "GeminiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val DEFAULT_MODEL = "gemini-3.5-flash"
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(customKey: String?): String {
        val trimmedCustom = customKey?.trim()
        if (!trimmedCustom.isNullOrEmpty()) {
            return trimmedCustom
        }
        val prefs = context.getSharedPreferences("habib_settings", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("custom_gemini_api_key", null)?.trim()
        if (!savedKey.isNullOrEmpty()) {
            return savedKey
        }
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    private fun buildSystemInstruction(userProfile: UserEntity?): JSONObject {
        val instructionText = buildString {
            append("You are HABIB AI (হাবিব এআই), a warm, highly intelligent, respectful, and articulate AI companion built for Bengali and English speaking users. ")
            append("You excel at natural Bengali conversation with rich vocabulary, proper grammar, and empathetic tone. ")
            append("When addressed in Bengali, respond in beautiful, natural Bengali (বাংলা). When addressed in English, respond in clear English. ")
            if (userProfile != null) {
                if (userProfile.preferredName.isNotBlank()) {
                    append("The user's preferred name is '${userProfile.preferredName}'. Address them respectfully by name when natural. ")
                }
                if (userProfile.educationLevel.isNotBlank()) {
                    append("User's educational level is '${userProfile.educationLevel}'. Calibrate explanations accordingly. ")
                }
                if (userProfile.district.isNotBlank()) {
                    append("User's location/district is '${userProfile.district}', Bangladesh. Keep regional context in mind. ")
                }
                when (userProfile.responseStyle) {
                    "concise" -> append("User prefers concise, direct, to-the-point answers. ")
                    "academic" -> append("User prefers structured, analytical, detailed educational answers. ")
                    else -> append("User prefers friendly, encouraging, and intelligent explanations. ")
                }
            }
            append("Ensure all Bengali conjuncts, punctuation, and idioms are completely correct. Never output offensive or false information.")
        }

        return JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", instructionText))
            })
        }
    }

    private fun buildContentsJson(
        history: List<ChatMessageEntity>,
        newPrompt: String,
        imageBase64: String?,
        mimeType: String?
    ): JSONArray {
        val contents = JSONArray()

        // Include last up to 10 context messages for conversational coherence
        val recentHistory = history.filter { it.sender == "user" || it.sender == "assistant" }.takeLast(10)

        for (msg in recentHistory) {
            val role = if (msg.sender == "user") "user" else "model"
            val parts = JSONArray()

            // If historical message had an image
            if (msg.imageBase64 != null) {
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", msg.imageBase64)
                }
                parts.put(JSONObject().put("inlineData", inlineData))
            }

            if (msg.text.isNotBlank()) {
                parts.put(JSONObject().put("text", msg.text))
            }

            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", parts)
            })
        }

        // Current turn
        val currentTurnParts = JSONArray()
        if (!imageBase64.isNullOrEmpty()) {
            val inlineData = JSONObject().apply {
                put("mimeType", mimeType ?: "image/jpeg")
                put("data", imageBase64)
            }
            currentTurnParts.put(JSONObject().put("inlineData", inlineData))
        }

        if (newPrompt.isNotBlank()) {
            currentTurnParts.put(JSONObject().put("text", newPrompt))
        }

        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", currentTurnParts)
        })

        return contents
    }

    suspend fun streamGenerateContent(
        history: List<ChatMessageEntity>,
        newPrompt: String,
        imageBase64: String? = null,
        mimeType: String? = "image/jpeg",
        userProfile: UserEntity? = null,
        customApiKey: String? = null,
        onChunk: (String) -> Unit
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult.Failure(
                bengaliMessage = "Gemini API কী কনফিগার করা হয়নি! দয়া করে AI Studio Secrets প্যানেল অথবা অ্যাপের সেটিংস থেকে আপনার Gemini API Key প্রবেশ করান।",
                technicalDetail = "API Key is missing or default placeholder.",
                isConfigError = true
            )
        }

        val requestJson = JSONObject().apply {
            put("systemInstruction", buildSystemInstruction(userProfile))
            put("contents", buildContentsJson(history, newPrompt, imageBase64, mimeType))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("topK", 40)
            })
        }

        val url = "$BASE_URL/$DEFAULT_MODEL:streamGenerateContent?alt=sse&key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Accept", "text/event-stream")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                val code = response.code
                val bengali = mapHttpError(code, errorBody)
                return@withContext GeminiResult.Failure(
                    bengaliMessage = bengali,
                    technicalDetail = "HTTP $code: $errorBody",
                    isConfigError = code in 401..403
                )
            }

            val body = response.body ?: return@withContext GeminiResult.Failure(
                bengaliMessage = "সার্ভার থেকে কোনো তথ্য পাওয়া যায়নি।",
                technicalDetail = "Empty response body"
            )

            val fullTextBuilder = StringBuilder()
            BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (trimmed.startsWith("data:")) {
                        val payload = trimmed.removePrefix("data:").trim()
                        if (payload.isEmpty() || payload == "[DONE]") continue
                        try {
                            val chunkJson = JSONObject(payload)
                            val candidates = chunkJson.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val firstCandidate = candidates.getJSONObject(0)
                                val content = firstCandidate.optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts != null && parts.length() > 0) {
                                    for (i in 0 until parts.length()) {
                                        val part = parts.getJSONObject(i)
                                        val chunkText = part.optString("text")
                                        if (chunkText.isNotEmpty()) {
                                            fullTextBuilder.append(chunkText)
                                            onChunk(chunkText)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing SSE chunk: ${e.message}")
                        }
                    }
                }
            }

            val finalOutput = fullTextBuilder.toString()
            if (finalOutput.isEmpty()) {
                GeminiResult.Failure(
                    bengaliMessage = "মডেল কোনো উত্তর প্রদান করতে পারেনি।",
                    technicalDetail = "Model returned empty text candidate."
                )
            } else {
                GeminiResult.Success(finalOutput)
            }
        } catch (e: UnknownHostException) {
            GeminiResult.Failure(
                bengaliMessage = "ইন্টারনেট সংযোগ নেই অথবা Gemini সার্ভার পৌঁছানো যাচ্ছে না। দয়া করে আপনার নেটওয়ার্ক চেক করুন।",
                technicalDetail = e.localizedMessage ?: "UnknownHostException"
            )
        } catch (e: Exception) {
            GeminiResult.Failure(
                bengaliMessage = "সংযোগ বিচ্ছিন্ন হয়েছে: ${e.message ?: "অজানা ত্রুটি"}",
                technicalDetail = e.localizedMessage ?: "General Exception"
            )
        }
    }

    suspend fun testConnection(customApiKey: String? = null): ConnectionDiagnosticResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ConnectionDiagnosticResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = 0,
                message = "API কী পাওয়া যায়নি। সেটিংস থেকে সঠিক Gemini API Key প্রদান করুন।"
            )
        }

        val startTime = System.currentTimeMillis()
        val testJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Hello"))
                    })
                })
            })
        }

        val url = "$BASE_URL/$DEFAULT_MODEL:generateContent?key=$apiKey"
        val requestBody = testJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        try {
            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val code = response.code

            if (response.isSuccessful) {
                ConnectionDiagnosticResult(
                    isSuccess = true,
                    statusCode = code,
                    latencyMs = latency,
                    message = "Gemini API সফলভাবে সংযুক্ত ($latency ms)"
                )
            } else {
                val err = response.body?.string().orEmpty()
                ConnectionDiagnosticResult(
                    isSuccess = false,
                    statusCode = code,
                    latencyMs = latency,
                    message = mapHttpError(code, err)
                )
            }
        } catch (e: UnknownHostException) {
            ConnectionDiagnosticResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = 0,
                message = "ইন্টারনেট সংযোগ নেই। হোস্টের সাথে যোগাযোগ সম্ভব হয়নি।"
            )
        } catch (e: Exception) {
            ConnectionDiagnosticResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = 0,
                message = "পরীক্ষায় ত্রুটি: ${e.message}"
            )
        }
    }

    private fun mapHttpError(code: Int, body: String): String {
        return when (code) {
            400 -> "অনুরোধের বিন্যাস সঠিক নয় বা অসমর্থিত মডেল।"
            401, 403 -> "API কী অবৈধ বা অননুমোদিত। অনুগ্রহ করে বৈধ API Key নিশ্চিত করুন।"
            429 -> "অনুরোধের কোটা বা রেট লিমিট শেষ হয়ে গেছে (Quota Exceeded)।"
            500, 503 -> "Google Gemini সার্ভারে সাময়িক সমস্যা চলছে।"
            else -> "সার্ভার প্রতিক্রিয়া কোড $code: $body"
        }
    }
}

data class ConnectionDiagnosticResult(
    val isSuccess: Boolean,
    val statusCode: Int,
    val latencyMs: Long,
    val message: String
)
