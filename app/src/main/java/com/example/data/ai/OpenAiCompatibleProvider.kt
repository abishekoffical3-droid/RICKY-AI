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

class OpenAiCompatibleProvider(
    private val apiKeyProvider: () -> String,
    private val baseUrlProvider: () -> String
) : AiProvider {

    override val id: String = "OPENAI_COMPATIBLE"
    override val displayName: String = "OpenAI / OpenRouter / Custom"
    override val availableModels: List<String> = listOf(
        "meta-llama/llama-3.3-70b-instruct",
        "mistralai/mistral-large-2411",
        "gpt-4o-mini",
        "deepseek/deepseek-chat"
    )
    override val supportsVision: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun testConnection(): Result<String> {
        val key = apiKeyProvider()
        val base = baseUrlProvider().trimEnd('/')
        if (base.isBlank()) return Result.failure(Exception("Base URL is empty."))
        return try {
            val url = "$base/models"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $key")
                .get()
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    Result.success("Connection to $base successful!")
                } else {
                    Result.failure(Exception("HTTP ${res.code}: ${res.body?.string()}"))
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
        val base = baseUrlProvider().trimEnd('/')
        val endpoint = "$base/chat/completions"

        val json = JSONObject()
        val targetModel = model.ifBlank { "meta-llama/llama-3.3-70b-instruct" }
        json.put("model", targetModel)
        json.put("stream", true)

        val messages = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
        }
        for (m in history.takeLast(10)) {
            messages.put(JSONObject().apply {
                put("role", m.role)
                put("content", m.content)
            })
        }

        if (!imageBase64.isNullOrBlank()) {
            val userContent = JSONArray()
            userContent.put(JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            })
            val mime = mimeType ?: "image/jpeg"
            userContent.put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().put("url", "data:$mime;base64,$imageBase64"))
            })
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", userContent)
            })
        } else {
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }
        json.put("messages", messages)

        val req = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val res = client.newCall(req).execute()
            if (!res.isSuccessful) {
                val err = res.body?.string().orEmpty()
                emit(AiChunk(text = "API Error (HTTP ${res.code}): $err", isDone = true, error = err))
                return@flow
            }
            val stream = res.body?.byteStream() ?: run {
                emit(AiChunk(text = "Empty response stream.", isDone = true))
                return@flow
            }
            val reader = BufferedReader(InputStreamReader(stream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (l.startsWith("data: ")) {
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val obj = JSONObject(data)
                        val choices = obj.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val content = delta?.optString("content", "")
                            if (!content.isNullOrEmpty()) {
                                emit(AiChunk(text = content))
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("OpenAiProvider", "Chunk parse error: ${e.message}")
                    }
                }
            }
            emit(AiChunk(text = "", isDone = true))
        } catch (e: Exception) {
            emit(AiChunk(text = "\n\n[Connection Error: ${e.localizedMessage}]", isDone = true, error = e.message))
        }
    }.flowOn(Dispatchers.IO)
}
