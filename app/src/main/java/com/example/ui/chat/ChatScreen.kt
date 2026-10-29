package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ChatMessageEntity
import com.example.data.search.Citation
import com.example.ui.components.CitationCard
import com.example.ui.components.MarkdownText
import com.example.ui.theme.RickyCyan
import com.example.ui.theme.RickyDarkSurface
import com.example.ui.theme.RickyDarkSurfaceVariant
import com.example.ui.theme.RickyPurple
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.GenerationState
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    val isWebResearchEnabled by viewModel.isWebResearchEnabled.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val activeCitations by viewModel.activeCitations.collectAsState()
    val activeModel by viewModel.activeModelName.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll on new messages
    LaunchedEffect(messages.size, generationState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo picker for multimodal vision queries
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.setSelectedImage(uri)
    }

    // Microphone permission launcher for voice input
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.voiceManager.startListening()
        } else {
            Toast.makeText(context, "Microphone permission required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(RickyCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("R", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("RICKY AI", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(activeModel.take(15), fontSize = 10.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("menu_button")) {
                        Icon(Icons.Default.Menu, contentDescription = "Open drawer")
                    }
                },
                actions = {
                    // Web research toggle button
                    IconButton(
                        onClick = { viewModel.setWebResearchEnabled(!isWebResearchEnabled) },
                        modifier = Modifier.testTag("web_research_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Toggle web research",
                            tint = if (isWebResearchEnabled) RickyCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Hands-free voice mode button
                    IconButton(onClick = onNavigateToVoice, modifier = Modifier.testTag("voice_mode_button")) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Voice mode", tint = RickyPurple)
                    }
                    // New chat button
                    IconButton(onClick = { viewModel.startNewConversation() }, modifier = Modifier.testTag("new_chat_button")) {
                        Icon(Icons.Default.AddComment, contentDescription = "New chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    val msg = inputText
                    inputText = ""
                    viewModel.sendMessage(msg)
                },
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onStartVoice = {
                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                },
                selectedImageUri = selectedImageUri,
                onClearImage = { viewModel.setSelectedImage(null) },
                isGenerating = generationState !is GenerationState.Idle,
                onCancelGenerating = { viewModel.cancelGeneration() },
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (messages.isEmpty() && generationState is GenerationState.Idle) {
                EmptyChatWelcome(
                    onPromptClick = { prompt ->
                        viewModel.sendMessage(prompt)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageItem(message = message)
                    }

                    // Active streaming or thinking item
                    when (val state = generationState) {
                        is GenerationState.Researching -> {
                            item {
                                ResearchingCard(query = state.query)
                            }
                        }
                        is GenerationState.Thinking -> {
                            item {
                                ThinkingIndicator()
                            }
                        }
                        is GenerationState.Streaming -> {
                            item {
                                StreamingAssistantBubble(
                                    text = state.accumulatedText,
                                    citations = activeCitations
                                )
                            }
                        }
                        is GenerationState.Error -> {
                            item {
                                ErrorCard(message = state.message)
                            }
                        }
                        GenerationState.Idle -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessageEntity) {
    val context = LocalContext.current
    val isUser = message.role == "user"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(RickyCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text("R", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag(if (isUser) "user_message" else "assistant_message")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.imageUri != null) {
                        AsyncImage(
                            model = message.imageUri,
                            contentDescription = "User attached image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .padding(bottom = 8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (isUser) {
                        Text(
                            text = message.content,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )
                    } else {
                        MarkdownText(content = message.content)

                        // Parse citations from JSON if present
                        if (!message.citationsJson.isNullOrBlank()) {
                            val citations = remember(message.citationsJson) {
                                val list = mutableListOf<Citation>()
                                try {
                                    val arr = JSONArray(message.citationsJson)
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        list.add(
                                            Citation(
                                                index = obj.getInt("index"),
                                                title = obj.getString("title"),
                                                url = obj.getString("url"),
                                                snippet = obj.optString("snippet", "")
                                            )
                                        )
                                    }
                                } catch (_: Exception) {}
                                list
                            }

                            if (citations.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "📚 Sources & Citations",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RickyCyan
                                )
                                for (citation in citations) {
                                    CitationCard(citation = citation)
                                }
                            }
                        }

                        // Copy response action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("response", message.content))
                                    Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp).testTag("copy_response_button")
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy message",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, message.content)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share RICKY AI Response"))
                                },
                                modifier = Modifier.size(24.dp).testTag("share_response_button")
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share message",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingAssistantBubble(
    text: String,
    citations: List<Citation>
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(RickyCyan),
            contentAlignment = Alignment.Center
        ) {
            Text("R", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                MarkdownText(content = text)
                if (citations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📚 Researched Sources",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RickyCyan
                    )
                    for (c in citations) {
                        CitationCard(citation = c)
                    }
                }
            }
        }
    }
}

@Composable
fun ResearchingCard(query: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = RickyDarkSurfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = RickyCyan,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Investigating online sources...",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = RickyCyan
                )
                Text(
                    text = query,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ThinkingIndicator() {
    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = RickyCyan,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "RICKY is thinking...",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    onStartVoice: () -> Unit,
    selectedImageUri: Uri?,
    onClearImage: () -> Unit,
    isGenerating: Boolean,
    onCancelGenerating: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Preview selected image thumbnail
            if (selectedImageUri != null) {
                Box(modifier = Modifier.padding(bottom = 6.dp)) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image preview",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onClearImage,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear image", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Attach image button
                IconButton(onClick = onPickImage, modifier = Modifier.testTag("attach_image_button")) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Input TextField
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Ask RICKY anything... (नेपाली / Eng / हिंदी)", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 120.dp)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RickyCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Send or Cancel or Mic
                if (isGenerating) {
                    IconButton(
                        onClick = onCancelGenerating,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .testTag("cancel_generation_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                    }
                } else if (text.isNotBlank() || selectedImageUri != null) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .background(RickyCyan, CircleShape)
                            .testTag("send_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                    }
                } else {
                    IconButton(
                        onClick = onStartVoice,
                        modifier = Modifier
                            .background(RickyPurple, CircleShape)
                            .testTag("mic_button")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatWelcome(onPromptClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(RickyDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(RickyCyan),
                contentAlignment = Alignment.Center
            ) {
                Text("R", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 32.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to RICKY AI",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Online-first personal assistant with web research, citations, persistent memory, and practical tools.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        val samplePrompts = listOf(
            "Who are you and what are your capabilities?",
            "What is the latest news and weather in Nepal?",
            "Calculate sqrt(256) * 14 + 180 / 4",
            "नेपालीमा नमस्ते भन्नुहोस् र मलाई मद्दत गर्नुहोस्"
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            for (p in samplePrompts) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onPromptClick(p) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RickyCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = p, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
