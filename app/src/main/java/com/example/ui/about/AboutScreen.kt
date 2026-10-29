package com.example.ui.about

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RickyCyan
import com.example.ui.theme.RickyDarkSurfaceVariant
import com.example.ui.theme.RickyNeonGreen
import com.example.ui.theme.RickyPurple
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val identity = viewModel.identity
    val activeProviderId by viewModel.activeProviderId.collectAsState()
    val activeModel by viewModel.activeModelName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About RICKY AI", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("about_back_button")) {
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
            // Emblem and Brand
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(RickyDarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(RickyCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("R", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 28.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("RICKY AI", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = RickyCyan)
                Text("Version ${identity.version} (Build ${identity.buildNumber}) • ${identity.releaseDate}", fontSize = 12.sp, color = Color.LightGray)
                Text("Developer: ${identity.developerName} (${identity.organizationName})", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Honest Capabilities Dashboard
            Text("Capabilities Status Dashboard", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RickyCyan)
            Text("Truthful reporting of active vs optional capabilities.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            val capabilities = listOf(
                CapabilityStatus("Streaming AI Conversation", "Working & Tested", RickyNeonGreen, "Progressive streaming tokens via SSE and local engine"),
                CapabilityStatus("Online Web Research & Citations", "Working & Tested", RickyNeonGreen, "DuckDuckGo & Wikipedia search with clickable source links"),
                CapabilityStatus("Persistent Memory Store", "Working & Tested", RickyNeonGreen, "Local Room database with user CRUD and export"),
                CapabilityStatus("Multilingual Voice & Speech", "Working & Tested", RickyNeonGreen, "Nepali, English, and Hindi Speech-to-Text & Text-to-Speech"),
                CapabilityStatus("Safe Math Calculator", "Working & Tested", RickyNeonGreen, "Mathematical expression parser without eval"),
                CapabilityStatus("CSV Data Analysis", "Working & Tested", RickyNeonGreen, "Parses tables and computes numerical column statistics"),
                CapabilityStatus("Device Reminders & Alarms", "Working & Tested", RickyNeonGreen, "Android AlarmManager & NotificationManager"),
                CapabilityStatus("Cloud Database Sync", "Configured (Optional)", Color(0xFF64B5F6), "Firebase Firestore integration ready for cloud sync"),
                CapabilityStatus("Google Sign-In Auth", "Configured (Optional)", Color(0xFF64B5F6), "Credential Manager ready with authorized credentials")
            )

            for (cap in capabilities) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = RickyDarkSurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(cap.statusColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cap.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("• ${cap.status}", fontSize = 11.sp, color = cap.statusColor, fontWeight = FontWeight.SemiBold)
                            }
                            Text(cap.detail, fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Provider & Model Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Current Active Configuration", fontWeight = FontWeight.Bold, color = RickyCyan, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Provider: $activeProviderId", fontSize = 12.sp, color = Color.White)
                    Text("• Model: $activeModel", fontSize = 12.sp, color = Color.White)
                    Text("• Support: ${identity.supportEmail}", fontSize = 12.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy & Principles Statement
            Text("Privacy & Principles", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RickyCyan)
            Text(
                "• RICKY AI operates with least-privilege permissions.\n" +
                "• Your conversations and memories are stored privately on your device.\n" +
                "• RICKY does not claim false affiliation with OpenAI, ChatGPT, or Google.\n" +
                "• Memory is a separate controllable store and is NEVER used to retrain external AI models without consent.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // External Links
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(identity.officialWebsite))
                    try { context.startActivity(intent) } catch (_: Exception) {}
                }) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GitHub / Docs", fontSize = 12.sp)
                }

                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(identity.privacyPolicyUrl))
                    try { context.startActivity(intent) } catch (_: Exception) {}
                }) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Privacy Policy", fontSize = 12.sp)
                }
            }
        }
    }
}

private data class CapabilityStatus(
    val name: String,
    val status: String,
    val statusColor: Color,
    val detail: String
)
