package com.example.data.ai

import com.example.core.identity.RickyIdentity
import com.example.feature.tools.SafeCalculator
import com.example.data.ai.localmodel.OnDeviceModelRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Intelligent local assistant fallback for offline / edge usage.
 * Answers identity queries truthfully, handles safe calculations,
 * and provides responsive multi-lingual guidance in Nepali, Hindi, and English.
 */
class LocalAssistantProvider(
    private val identity: RickyIdentity = RickyIdentity(),
    private val modelRunner: OnDeviceModelRunner? = null
) : AiProvider {

    override val id: String = "LOCAL_ASSISTANT"
    override val displayName: String = "RICKY Local Engine (On-Device)"
    override val availableModels: List<String> = listOf("ricky-char-gpt-v1", "ricky-rule-fallback-v1")
    override val supportsVision: Boolean = false

    override suspend fun testConnection(): Result<String> {
        return Result.success(if (modelRunner?.isModelInstalled() == true) "Scratch-trained RICKY ONNX model is installed and ready offline." else "Offline tools are ready. No trained neural model is bundled yet; install a trained ONNX model to enable neural text generation.")
    }

    override fun streamResponse(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        imageBase64: String?,
        mimeType: String?
    ): Flow<AiChunk> = flow {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase()

        // Prefer the user's own scratch-trained model when the exported ONNX assets exist.
        // The rule-based fallback remains available when no model has been trained/exported.
        if (modelRunner?.isModelInstalled() == true && !SafeCalculator.isMathExpression(trimmed)) {
            val contextText = buildString {
                history.takeLast(8).forEach { message ->
                    val label = if (message.role.equals("assistant", true)) "RICKY" else "User"
                    append(label).append(": ").append(message.content.take(800)).append("\n")
                }
                append("User: ").append(trimmed.take(1200)).append("\nRICKY:")
            }
            val generated = withContext(Dispatchers.Default) { modelRunner.generate(contextText) }
            if (!generated.isNullOrBlank()) {
                streamText(generated) { emit(it) }
                emit(AiChunk(text = "", isDone = true))
                return@flow
            }
        }

        // 1. Check if user is asking for math calculation
        if (SafeCalculator.isMathExpression(trimmed)) {
            val calcResult = SafeCalculator.evaluate(trimmed)
            val response = when (calcResult) {
                is SafeCalculator.CalcResult.Success -> "🧮 **Calculator Result:**\n\n`$trimmed` = **${calcResult.formattedResult}**"
                is SafeCalculator.CalcResult.Error -> "⚠️ Calculation error: ${calcResult.message}"
            }
            streamText(response) { emit(it) }
            emit(AiChunk(text = "", isDone = true))
            return@flow
        }

        // 2. Identity Queries
        if (lower.contains("who are you") || lower.contains("your name") || lower.contains("who made you") || lower.contains("तपाईं को हो") || lower.contains("timro naam ke ho") || lower.contains("kaha bata")) {
            val answer = """
                👋 Hello! I am **${identity.productName}** (v${identity.version}), your personal AI assistant.
                
                - **Developer**: ${identity.developerName} (${identity.organizationName})
                - **Primary Languages**: Nepali (नेपाली), English, and Hindi (हिन्दी)
                - **Active Mode**: Local On-Device Mode
                
                I can help with offline calculations, writing practice, coding examples, and your saved local notes. If your own trained RICKY model is installed, I can also use that model without an API key.
            """.trimIndent()
            streamText(answer) { emit(it) }
            emit(AiChunk(text = "", isDone = true))
            return@flow
        }

        // 3. Nepali Greetings & Assistance
        if (lower.contains("namaste") || lower.contains("नमस्ते") || lower.contains("sanchai") || lower.contains("सञ्चै")) {
            val answer = """
                नमस्ते! म **RICKY AI** हुँ। म सञ्चै छु, तपाईंलाई कस्तो छ?
                
                म तपाईंलाई निम्न कार्यहरूमा मद्दत गर्न सक्छु:
                - 🌐 अनलाइन खोज र जानकारी (Web Research)
                - 🧮 गणितीय हिसाब (Math Calculator)
                - ✍️ लेख तथा अनुवाद (Writing & Translation)
                - 💻 प्रोग्रामिङ र कोडिङ (Coding Assistant)
                - ⏰ रिमाइन्डर र नोटहरू (Reminders & Memories)
                
                तपाईंलाई आज के सहयोग चाहिन्छ?
            """.trimIndent()
            streamText(answer) { emit(it) }
            emit(AiChunk(text = "", isDone = true))
            return@flow
        }

        // 4. Hindi Greetings
        if (lower.contains("namaste") || lower.contains("namaskar") || lower.contains("kya haal hai") || lower.contains("नमस्ते")) {
            val answer = """
                नमस्ते! मैं **RICKY AI** हूँ, आपका व्यक्तिगत AI सहायक।
                
                मैं आपकी मदद कर सकता हूँ:
                - वेब रिसर्च और तथ्यात्मक जानकारी
                - गणित और कैलकुलेटर
                - कोडिंग और प्रोग्रामिंग
                - हिंदी, नेपाली और अंग्रेज़ी में अनुवाद
                - व्यक्तिगत नोट्स और रिमाइंडर्स
                
                बताइए, मैं आपकी क्या सहायता करूँ?
            """.trimIndent()
            streamText(answer) { emit(it) }
            emit(AiChunk(text = "", isDone = true))
            return@flow
        }

        // 5. Default General Response
        val defaultResponse = buildString {
            appendLine("Hello! I received your query:")
            appendLine("> *$trimmed*")
            appendLine()
            appendLine("I am running in **Local On-Device Mode**. This response is from the built-in offline fallback, not a large trained neural model.")
            appendLine("To enable RICKY's scratch-trained neural model, train it with the `training/` project, export it to ONNX, then add `model.onnx`, `vocab.json`, and `model.metadata.json` under `app/src/main/assets/ricky/` and rebuild.")
            appendLine()
            appendLine("💡 **Available offline tools right now:**")
            appendLine("• **Calculator**: Type mathematical calculations like `sqrt(144) + 25 * 3`")
            appendLine("• **Tools Hub**: Use the Tools tab for Writing, Coding, and CSV data analysis")
            appendLine("• **Memories**: Store and review your personal notes and project goals")
        }
        streamText(defaultResponse) { emit(it) }
        emit(AiChunk(text = "", isDone = true))
    }

    private suspend fun streamText(fullText: String, onChunk: suspend (AiChunk) -> Unit) {
        val words = fullText.split(" ")
        val buffer = StringBuilder()
        for (i in words.indices) {
            buffer.append(words[i]).append(" ")
            if (i % 3 == 0 || i == words.size - 1) {
                onChunk(AiChunk(text = buffer.toString()))
                buffer.clear()
                delay(30)
            }
        }
    }
}
