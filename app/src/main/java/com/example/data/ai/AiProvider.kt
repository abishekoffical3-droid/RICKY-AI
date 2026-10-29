package com.example.data.ai

import kotlinx.coroutines.flow.Flow

data class ChatMessage(
    val role: String, // "user", "assistant", "system"
    val content: String,
    val imageBase64: String? = null,
    val mimeType: String? = null
)

data class AiChunk(
    val text: String,
    val isDone: Boolean = false,
    val error: String? = null
)

interface AiProvider {
    val id: String
    val displayName: String
    val availableModels: List<String>
    val supportsVision: Boolean

    suspend fun testConnection(): Result<String>

    fun streamResponse(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        imageBase64: String? = null,
        mimeType: String? = null
    ): Flow<AiChunk>
}
