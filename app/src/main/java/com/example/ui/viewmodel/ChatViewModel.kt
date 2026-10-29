package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.identity.RickyIdentity
import com.example.core.identity.UserProfile
import com.example.data.ai.*
import com.example.data.local.*
import com.example.data.search.Citation
import com.example.data.search.ResearchEngine
import com.example.feature.tools.SafeCalculator
import com.example.feature.voice.VoiceManager
import com.example.feature.voice.VoiceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.util.UUID

sealed interface GenerationState {
    data object Idle : GenerationState
    data object Thinking : GenerationState
    data class Researching(val query: String) : GenerationState
    data class Streaming(val accumulatedText: String) : GenerationState
    data class Error(val message: String) : GenerationState
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RickyDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val memoryDao = db.memoryDao()
    val preferencesManager = AppPreferencesManager(application)
    val identity = RickyIdentity()

    val providerRegistry = AiProviderRegistry(preferencesManager, identity, application)
    val researchEngine = ResearchEngine()
    val voiceManager = VoiceManager(application)

    // Current Active Conversation
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    // All Conversations List
    val conversations: StateFlow<List<ConversationEntity>> = chatDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Messages for current conversation
    val messages: StateFlow<List<ChatMessageEntity>> = _currentConversationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else chatDao.getMessagesForConversation(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Generation UI state
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    // Web Research toggle (User can manually force or let auto-detect)
    private val _isWebResearchEnabled = MutableStateFlow(false)
    val isWebResearchEnabled: StateFlow<Boolean> = _isWebResearchEnabled.asStateFlow()

    // Selected image attachment for multimodal query
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // Current citations for the active streaming message
    private val _activeCitations = MutableStateFlow<List<Citation>>(emptyList())
    val activeCitations: StateFlow<List<Citation>> = _activeCitations.asStateFlow()

    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val activeProviderId: StateFlow<String> = preferencesManager.activeAiProviderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "LOCAL_ASSISTANT")

    val activeModelName: StateFlow<String> = preferencesManager.activeAiModelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ricky-char-gpt-v1")

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState

    private var generationJob: Job? = null

    init {
        // Initialize default conversation if needed
        viewModelScope.launch {
            val existing = conversations.first()
            if (existing.isNotEmpty()) {
                _currentConversationId.value = existing.first().id
            } else {
                startNewConversation()
            }
        }

        voiceManager.onSpeechRecognized = { text ->
            sendMessage(text)
        }
    }

    fun setWebResearchEnabled(enabled: Boolean) {
        _isWebResearchEnabled.value = enabled
    }

    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun selectConversation(id: String) {
        _currentConversationId.value = id
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val newConv = ConversationEntity(
                id = newId,
                title = "New Chat",
                modelUsed = activeModelName.value
            )
            chatDao.insertConversation(newConv)
            _currentConversationId.value = newId
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatDao.deleteMessagesForConversation(id)
            chatDao.deleteConversation(id)
            if (_currentConversationId.value == id) {
                val remaining = conversations.first().filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    _currentConversationId.value = remaining.first().id
                } else {
                    startNewConversation()
                }
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _generationState.value = GenerationState.Idle
        voiceManager.stopSpeaking()
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() && _selectedImageUri.value == null) return

        val convId = _currentConversationId.value ?: return

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _activeCitations.value = emptyList()

            // 1. Convert attached image to base64 if present
            var base64Image: String? = null
            var mimeType: String? = null
            val imageUri = _selectedImageUri.value
            if (imageUri != null) {
                try {
                    val stream = getApplication<Application>().contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
                    base64Image = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    mimeType = "image/jpeg"
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Image conversion failed: ${e.message}")
                }
            }

            // Save user message to Room
            val userMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "user",
                content = trimmed,
                imageUri = imageUri?.toString()
            )
            chatDao.insertMessage(userMsg)
            _selectedImageUri.value = null

            // Update conversation title if first message
            val currentMsgs = chatDao.getMessagesList(convId)
            if (currentMsgs.size <= 2) {
                val title = if (trimmed.length > 28) trimmed.take(28) + "..." else trimmed
                val conv = chatDao.getConversationById(convId)
                if (conv != null) {
                    chatDao.updateConversation(conv.copy(title = title, updatedAt = System.currentTimeMillis()))
                }
            }

            // 2. Fast Safe Calculator Check
            if (SafeCalculator.isMathExpression(trimmed)) {
                val calcRes = SafeCalculator.evaluate(trimmed)
                val text = when (calcRes) {
                    is SafeCalculator.CalcResult.Success -> "🧮 **Calculator Result:**\n\n`$trimmed` = **${calcRes.formattedResult}**"
                    is SafeCalculator.CalcResult.Error -> "⚠️ Calculation error: ${calcRes.message}"
                }
                val assistantMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "assistant",
                    content = text
                )
                chatDao.insertMessage(assistantMsg)
                _generationState.value = GenerationState.Idle
                return@launch
            }

            // 3. Web Research Workflow
            var researchNotes = ""
            var citations = emptyList<Citation>()
            val doResearch = _isWebResearchEnabled.value || researchEngine.shouldResearch(trimmed)
            if (doResearch) {
                _generationState.value = GenerationState.Researching(trimmed)
                try {
                    val report = researchEngine.conductResearch(trimmed)
                    if (report.citations.isNotEmpty()) {
                        citations = report.citations
                        _activeCitations.value = citations
                        researchNotes = "\n\n" + report.synthesisNotes
                    }
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Research failed: ${e.message}")
                }
            }

            // 4. Retrieve Active Memories to augment system instruction
            val memories = memoryDao.getActiveMemories()
            val memoriesContext = if (memories.isNotEmpty() && userProfile.value.memoryEnabled) {
                "\nUser Approved Stored Memories:\n" + memories.joinToString("\n") { "- [${it.category}] ${it.key}: ${it.content}" }
            } else ""

            val provider = providerRegistry.getActiveProvider()
            val modelName = activeModelName.value
            val profile = userProfile.value
            val systemPrompt = identity.getSystemPrompt(profile, provider.displayName, modelName) + memoriesContext + researchNotes

            // Convert Room messages to ChatMessage history
            val history = currentMsgs.map {
                ChatMessage(role = it.role, content = it.content)
            }

            _generationState.value = GenerationState.Thinking
            val accumulated = StringBuilder()

            try {
                provider.streamResponse(
                    prompt = trimmed,
                    history = history,
                    systemInstruction = systemPrompt,
                    model = modelName,
                    imageBase64 = base64Image,
                    mimeType = mimeType
                ).collect { chunk ->
                    if (chunk.error != null) {
                        _generationState.value = GenerationState.Error(chunk.error)
                        accumulated.append(chunk.text)
                    } else {
                        accumulated.append(chunk.text)
                        _generationState.value = GenerationState.Streaming(accumulated.toString())
                    }
                }

                val finalOutput = accumulated.toString().trim()
                if (finalOutput.isNotEmpty()) {
                    // Serialize citations to JSON if present
                    val citationsJson = if (citations.isNotEmpty()) {
                        val arr = JSONArray()
                        for (c in citations) {
                            arr.put(org.json.JSONObject().apply {
                                put("index", c.index)
                                put("title", c.title)
                                put("url", c.url)
                                put("snippet", c.snippet)
                            })
                        }
                        arr.toString()
                    } else null

                    val assistantMsg = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = convId,
                        role = "assistant",
                        content = finalOutput,
                        citationsJson = citationsJson
                    )
                    chatDao.insertMessage(assistantMsg)

                    // Speak response if voice active
                    if (voiceState.value is VoiceState.Processing || voiceState.value is VoiceState.Speaking) {
                        voiceManager.speak(
                            finalOutput.take(300),
                            speed = profile.voiceSpeed,
                            pitch = profile.voicePitch,
                            languageCode = profile.preferredLanguage
                        )
                    }
                }
                _generationState.value = GenerationState.Idle
            } catch (e: Exception) {
                _generationState.value = GenerationState.Error(e.localizedMessage ?: "Generation failed")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
