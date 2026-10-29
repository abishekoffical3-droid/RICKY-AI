package com.example.data.ai

import android.content.Context
import com.example.core.identity.RickyIdentity
import com.example.data.ai.localmodel.OnDeviceModelRunner
import com.example.data.local.AppPreferencesManager
import kotlinx.coroutines.flow.first

class AiProviderRegistry(
    private val preferencesManager: AppPreferencesManager,
    private val identity: RickyIdentity = RickyIdentity(),
    context: Context
) {
    private val localProvider = LocalAssistantProvider(identity, OnDeviceModelRunner(context.applicationContext))

    private val geminiProvider = GeminiProvider(
        apiKeyProvider = { "" }
    )

    private val openAiProvider = OpenAiCompatibleProvider(
        apiKeyProvider = { "" },
        baseUrlProvider = { "https://openrouter.ai/api/v1" }
    )

    suspend fun getActiveProvider(): AiProvider {
        val selectedId = preferencesManager.activeAiProviderFlow.first()
        return when (selectedId) {
            "GEMINI_OFFICIAL" -> {
                // If Gemini key is empty, still return Gemini provider so it can guide user or fall back
                geminiProvider
            }
            "OPENAI_COMPATIBLE" -> openAiProvider
            "LOCAL_ASSISTANT" -> localProvider
            else -> localProvider
        }
    }

    fun getAllProviders(): List<AiProvider> = listOf(
        geminiProvider,
        openAiProvider,
        localProvider
    )

    fun getLocalProvider(): LocalAssistantProvider = localProvider
}
