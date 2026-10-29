package com.example.data.ai.localmodel

import android.content.Context
import org.json.JSONObject
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer

/**
 * Optional ONNX Runtime adapter for the scratch-trained RICKY character model.
 * Put model.onnx and vocab.json in app/src/main/assets/ricky/ after training/export.
 * When those files are absent, the app continues to use the honest rule-based fallback.
 */
class OnDeviceModelRunner(private val context: Context) {
    companion object {
        private const val MODEL_ASSET = "ricky/model.onnx"
        private const val VOCAB_ASSET = "ricky/vocab.json"
        private const val MAX_NEW_TOKENS = 180
    }

    @Volatile private var session: OrtSession? = null
    @Volatile private var tokens: List<String>? = null
    @Volatile private var tokenIds: Map<String, Int>? = null
    @Volatile private var eosId: Int = 2
    @Volatile private var maxSequenceLength: Int = 256
    private val environment: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }

    fun isModelInstalled(): Boolean = try {
        context.assets.open(MODEL_ASSET).use { }
        context.assets.open(VOCAB_ASSET).use { }
        true
    } catch (_: Exception) {
        false
    }

    @Synchronized
    private fun ensureLoaded(): Boolean {
        if (session != null && tokens != null && tokenIds != null) return true
        return try {
            val modelFile = java.io.File(context.cacheDir, "ricky-model.onnx")
            context.assets.open(MODEL_ASSET).use { input -> modelFile.outputStream().use { output -> input.copyTo(output) } }
            val vocabJson = context.assets.open(VOCAB_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(vocabJson)
            val array = root.getJSONArray("tokens")
            val loadedTokens = (0 until array.length()).map { array.getString(it) }
            val loadedIds = loadedTokens.mapIndexed { index, token -> token to index }.toMap()
            eosId = root.optInt("eos_token_id", 2)
            val options = OrtSession.SessionOptions()
            options.setIntraOpNumThreads(2)
            session = environment.createSession(modelFile.absolutePath, options)
            tokens = loadedTokens
            tokenIds = loadedIds
            val metadataFile = try {
                context.assets.open("ricky/model.metadata.json").bufferedReader().use { JSONObject(it.readText()) }
            } catch (_: Exception) { JSONObject() }
            maxSequenceLength = metadataFile.optInt("max_seq_len", 256).coerceAtLeast(16)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Returns null when no exported model is installed or inference fails. */
    fun generate(prompt: String, maxNewTokens: Int = MAX_NEW_TOKENS): String? {
        if (!ensureLoaded()) return null
        val currentSession = session ?: return null
        val vocab = tokens ?: return null
        val ids = tokenIds ?: return null
        val unk = ids["<unk>"] ?: 1
        val generated = prompt.map { ids[it.toString()] ?: unk }.toMutableList()
        val promptLength = generated.size
        return try {
            for (step in 0 until maxNewTokens) {
                val window = generated.takeLast(maxSequenceLength).map { it.toLong() }.toLongArray()
                val tensor = OnnxTensor.createTensor(
                    environment,
                    LongBuffer.wrap(window),
                    longArrayOf(1L, window.size.toLong())
                )
                val nextId = try {
                    currentSession.run(mapOf("input_ids" to tensor)).use { result ->
                        @Suppress("UNCHECKED_CAST")
                        val logits = result[0].value as Array<Array<FloatArray>>
                        val last = logits[0][logits[0].lastIndex]
                        last.indices.maxByOrNull { last[it] } ?: eosId
                    }
                } finally {
                    tensor.close()
                }
                if (nextId == eosId) break
                generated.add(nextId)
            }
            generated.drop(promptLength).mapNotNull { id -> vocab.getOrNull(id)?.takeIf { it.length == 1 } }
                .joinToString("").trim().ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}
