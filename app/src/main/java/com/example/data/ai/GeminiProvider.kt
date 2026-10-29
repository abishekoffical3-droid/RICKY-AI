package com.example.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val apiKeyProvider: () -> String
) : AiProvider {

    override val id: String = "GEMINI_OFFICIAL"
    override val displayName: String = "Google Gemini"
    override val availableModels: List<String> = listOf(
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite",
        "gemini-4-argon"
    )
    override val supportsVision: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun testConnection(): Result<String> {
        val key = apiKeyProvider()
        if (key.isBlank()) {
            return Result.failure(IllegalStateException("No Gemini API Key found. Configure in Settings or AI Studio Secrets."))
        }
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash?key=$key"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Connection successful! Gemini model verified.")
                } else {
                    val err = response.body?.string().orEmpty()
                    Result.failure(Exception("HTTP ${response.code}: $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun streamResponse(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        imageBase64: String?,
        mimeType: String?
    ): Flow<AiChunk> = flow {
        val key = apiKeyProvider()
        if (key.isBlank()) {
            emit(
                AiChunk(
                    text = "⚠️ Gemini API key is not configured. Please enter your API key in **Settings > AI Provider** or add it to Secrets in AI Studio.\n\nSwitching to RICKY Local Assistant to help you right now.",
                    isDone = true
                )
            )
            return@flow
        }

        val targetModel = if (model.isNotBlank()) model else "gemini-3.8-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:streamGenerateContent?alt=sse&key=$key"

        // Build Gemini Request Payload
        val jsonPayload = JSONObject()

        // System Instruction
        if (systemInstruction.isNotBlank()) {
            jsonPayload.put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })
        }

        // Contents
        val contentsArray = JSONArray()
        for (msg in history.takeLast(10)) {
            val role = if (msg.role == "assistant") "model" else "user"
            val partsArray = JSONArray().apply {
                put(JSONObject().put("text", msg.content))
            }
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", partsArray)
            })
        }

        // Current prompt with optional image
        val currentParts = JSONArray()
        if (!imageBase64.isNullOrBlank()) {
            val mime = mimeType ?: "image/jpeg"
            currentParts.put(JSONObject().apply {
                put("inline_data", JSONObject().apply {
                    put("mime_type", mime)
                    put("data", imageBase64)
                })
            })
        }
        currentParts.put(JSONObject().put("text", prompt))
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", currentParts)
        })

        jsonPayload.put("contents", contentsArray)

        // Generation Config
        jsonPayload.put("generationConfig", JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 4096)
        })

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Accept", "text/event-stream")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                emit(AiChunk(text = "API Error (HTTP ${response.code}): $err", isDone = true, error = err))
                return@flow
            }

            val source = response.body?.byteStream() ?: run {
                emit(AiChunk(text = "Empty response received from Gemini.", isDone = true))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(source))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.startsWith("data: ")) {
                    val dataJson = currentLine.removePrefix("data: ").trim()
                    if (dataJson == "[DONE]") {
                        break
                    }
                    try {
                        val obj = JSONObject(dataJson)
                        val candidates = obj.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text", "")
                                if (text.isNotEmpty()) {
                                    emit(AiChunk(text = text))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("GeminiProvider", "Error parsing chunk: ${e.message}")
                    }
                }
            }
            emit(AiChunk(text = "", isDone = true))
        } catch (e: Exception) {
            emit(AiChunk(text = "\n\n[Network connection error: ${e.localizedMessage}]", isDone = true, error = e.message))
        }
    }.flowOn(Dispatchers.IO)
}
