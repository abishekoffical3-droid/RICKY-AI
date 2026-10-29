package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.about.AboutScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.memory.MemoryScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.RickyCyan
import com.example.ui.theme.RickyDarkBg
import com.example.ui.theme.RickyDarkSurfaceVariant
import com.example.ui.theme.RickyPurple
import com.example.ui.tools.ToolsScreen
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.voice.VoiceScreen
import kotlinx.coroutines.launch

enum class ScreenDestination {
    CHAT,
    VOICE,
    TOOLS,
    MEMORY,
    PROFILE,
    SETTINGS,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ChatViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(ScreenDestination.CHAT) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val conversations by viewModel.conversations.collectAsState()
    val activeConvId by viewModel.currentConversationId.collectAsState()

    // Handle back button when on secondary screen
    if (currentScreen != ScreenDestination.CHAT) {
        BackHandler {
            currentScreen = ScreenDestination.CHAT
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = RickyDarkBg
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Header Brand
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(RickyCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("R", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("RICKY AI", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Text("Personal Assistant", fontSize = 11.sp, color = RickyCyan)
                        }
                    }

                    // New Chat Action
                    FilledTonalButton(
                        onClick = {
                            viewModel.startNewConversation()
                            currentScreen = ScreenDestination.CHAT
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("drawer_new_chat_btn"),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = RickyDarkSurfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = RickyCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Conversation", color = Color.White, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Chat History", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Conversation history list
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(conversations, key = { it.id }) { conv ->
                            val isSelected = conv.id == activeConvId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) RickyDarkSurfaceVariant else Color.Transparent)
                                    .clickable {
                                        viewModel.selectConversation(conv.id)
                                        currentScreen = ScreenDestination.CHAT
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = if (isSelected) RickyCyan else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = conv.title,
                                        fontSize = 13.sp,
                                        color = if (isSelected) RickyCyan else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteConversation(conv.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    // Bottom navigation destinations
                    val navItems = listOf(
                        Triple(ScreenDestination.TOOLS, "Tools Hub", Icons.Default.Build),
                        Triple(ScreenDestination.MEMORY, "Memory Store", Icons.Default.Psychology),
                        Triple(ScreenDestination.PROFILE, "My Profile", Icons.Default.Person),
                        Triple(ScreenDestination.SETTINGS, "Settings", Icons.Default.Settings),
                        Triple(ScreenDestination.ABOUT, "About RICKY", Icons.Default.Info)
                    )

                    for ((dest, label, icon) in navItems) {
                        NavigationDrawerItem(
                            icon = { Icon(icon, contentDescription = null, tint = if (currentScreen == dest) RickyCyan else Color.LightGray) },
                            label = { Text(label, color = if (currentScreen == dest) RickyCyan else Color.White, fontSize = 13.sp) },
                            selected = currentScreen == dest,
                            onClick = {
                                currentScreen = dest
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = RickyDarkSurfaceVariant,
                                unselectedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    ) {
        when (currentScreen) {
            ScreenDestination.CHAT -> ChatScreen(
                viewModel = viewModel,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onNavigateToVoice = { currentScreen = ScreenDestination.VOICE }
            )
            ScreenDestination.VOICE -> VoiceScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = ScreenDestination.CHAT }
            )
            ScreenDestination.TOOLS -> ToolsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = ScreenDestination.CHAT }
            )
            ScreenDestination.MEMORY -> MemoryScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = ScreenDestination.CHAT }
            )
            ScreenDestination.PROFILE -> ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = ScreenDestination.CHAT }
            )
            ScreenDestination.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = ScreenDestination.CHAT }
            )
            ScreenDestination.ABOUT -> AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = ScreenDestination.CHAT }
            )
        }
    }
}
