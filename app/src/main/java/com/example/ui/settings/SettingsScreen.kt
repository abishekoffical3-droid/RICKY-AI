package com.example.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RickyCyan
import com.example.ui.theme.RickyDarkSurfaceVariant
import com.example.ui.theme.RickyPurple
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = viewModel.preferencesManager

    var provider by remember { mutableStateOf("LOCAL_ASSISTANT") }
    var model by remember { mutableStateOf("ricky-char-gpt-v1") }
    var customApiKey by remember { mutableStateOf("") }
    var customBaseUrl by remember { mutableStateOf("https://openrouter.ai/api/v1") }
    var searchProvider by remember { mutableStateOf("DUCKDUCKGO") }
    var searchApiKey by remember { mutableStateOf("") }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        provider = prefs.activeAiProviderFlow.first()
        model = prefs.activeAiModelFlow.first()
        customApiKey = prefs.customApiKeyFlow.first()
        customBaseUrl = prefs.customBaseUrlFlow.first()
        searchProvider = prefs.searchProviderFlow.first()
        searchApiKey = prefs.searchApiKeyFlow.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Providers", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("AI Engine Provider", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RickyCyan)
            Text("Choose the active intelligence backend for RICKY AI.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))

            val providers = listOf(
                "GEMINI_OFFICIAL" to "Google Gemini (Official / Secrets)",
                "OPENAI_COMPATIBLE" to "OpenAI / OpenRouter / Custom API",
                "LOCAL_ASSISTANT" to "Local On-Device Engine (Offline)"
            )

            providers.forEach { (pId, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = provider == pId,
                        onClick = {
                            provider = pId
                            model = when (pId) {
                                "GEMINI_OFFICIAL" -> "gemini-3.8-flash"
                                "OPENAI_COMPATIBLE" -> "meta-llama/llama-3.3-70b-instruct"
                                else -> "ricky-char-gpt-v1"
                            }
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = RickyCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Active Model Identifier") },
                modifier = Modifier.fillMaxWidth().testTag("model_input_field"),
                shape = RoundedCornerShape(12.dp)
            )

            if (provider == "OPENAI_COMPATIBLE") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customBaseUrl,
                    onValueChange = { customBaseUrl = it },
                    label = { Text("Base Endpoint URL (e.g. https://openrouter.ai/api/v1)") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_base_url_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = { customApiKey = it },
                    label = { Text("API Key / Bearer Token") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_api_key_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Connection button
            OutlinedButton(
                onClick = {
                    isTesting = true
                    testStatus = "Testing provider connection..."
                    coroutineScope.launch {
                        val currentActive = viewModel.providerRegistry.getActiveProvider()
                        val res = currentActive.testConnection()
                        testStatus = if (res.isSuccess) {
                            "✅ ${res.getOrNull()}"
                        } else {
                            "❌ Connection failed: ${res.exceptionOrNull()?.message}"
                        }
                        isTesting = false
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("test_connection_btn")
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Provider Connection")
            }

            if (testStatus != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(testStatus.orEmpty(), fontSize = 12.sp, color = if (testStatus?.startsWith("✅") == true) RickyCyan else MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text("Web Research Provider", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RickyCyan)
            Text("DuckDuckGo and Wikipedia work out-of-the-box without requiring API keys.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            listOf("DUCKDUCKGO" to "DuckDuckGo & Wikipedia (Free / Public)", "TAVILY" to "Tavily Search API (Key Required)").forEach { (spId, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = searchProvider == spId,
                        onClick = { searchProvider = spId },
                        colors = RadioButtonDefaults.colors(selectedColor = RickyCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label, fontSize = 13.sp)
                }
            }

            if (searchProvider == "TAVILY") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchApiKey,
                    onValueChange = { searchApiKey = it },
                    label = { Text("Tavily API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        prefs.saveAiSettings(provider, model.trim(), customApiKey.trim(), customBaseUrl.trim())
                        prefs.saveSearchSettings(searchProvider, searchApiKey.trim())
                        Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("save_settings_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = RickyCyan, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Configuration", fontWeight = FontWeight.Bold)
            }
        }
    }
}
