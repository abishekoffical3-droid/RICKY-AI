package com.example.ui.memory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.local.MemoryEntity
import com.example.data.local.RickyDatabase
import com.example.ui.theme.RickyCyan
import com.example.ui.theme.RickyDarkSurfaceVariant
import com.example.ui.theme.RickyPurple
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { RickyDatabase.getInstance(context) }
    val memoryDao = db.memoryDao()
    val memories by memoryDao.getAllMemories().collectAsState(initial = emptyList())
    val userProfile by viewModel.userProfile.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("Preference") }
    var newKey by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory System", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("memory_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_memory_btn")) {
                        Icon(Icons.Default.Add, contentDescription = "Add memory")
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
                .padding(horizontal = 16.dp)
        ) {
            // Master personalization switch card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = RickyDarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Personalization & Memory", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RickyCyan)
                        Text(
                            text = if (userProfile.memoryEnabled) "Active: RICKY references your saved facts & preferences." else "Paused: RICKY will not use saved memories.",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                    Switch(
                        checked = userProfile.memoryEnabled,
                        onCheckedChange = { isEnabled ->
                            coroutineScope.launch {
                                viewModel.preferencesManager.saveUserProfile(userProfile.copy(memoryEnabled = isEnabled))
                            }
                        },
                        modifier = Modifier.testTag("memory_master_switch")
                    )
                }
            }

            // Export and Clear actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        val jsonArr = JSONArray()
                        for (m in memories) {
                            jsonArr.put(JSONObject().apply {
                                put("id", m.id)
                                put("category", m.category)
                                put("key", m.key)
                                put("content", m.content)
                                put("createdAt", m.createdAt)
                            })
                        }
                        val jsonStr = jsonArr.toString(2)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("memories_export", jsonStr))
                        Toast.makeText(context, "Exported ${memories.size} memories to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("export_memories_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export JSON", fontSize = 12.sp)
                }

                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            memoryDao.clearAllMemories()
                            Toast.makeText(context, "All memories cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (memories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No memories saved yet.", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tap + to add a preference, project note, or fact.", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(memories, key = { it.id }) { memory ->
                        MemoryItemCard(
                            memory = memory,
                            onToggle = { isChecked ->
                                coroutineScope.launch {
                                    memoryDao.updateMemory(memory.copy(isEnabled = isChecked))
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    memoryDao.deleteMemory(memory.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Saved Memory") },
            text = {
                Column {
                    Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Preference", "Project", "Interest", "Fact").forEach { cat ->
                            FilterChip(
                                selected = newCategory == cat,
                                onClick = { newCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Topic / Key (e.g. Favorite Language)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Details / Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank() && newContent.isNotBlank()) {
                            coroutineScope.launch {
                                memoryDao.insertMemory(
                                    MemoryEntity(
                                        category = newCategory,
                                        key = newKey.trim(),
                                        content = newContent.trim(),
                                        isEnabled = true
                                    )
                                )
                                showAddDialog = false
                                newKey = ""
                                newContent = ""
                            }
                        }
                    }
                ) {
                    Text("Save Memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MemoryItemCard(
    memory: MemoryEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = RickyDarkSurfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = memory.isEnabled,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = RickyCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(memory.category, fontSize = 10.sp) },
                        modifier = Modifier.height(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(memory.key, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RickyCyan)
                }
                Text(memory.content, fontSize = 12.sp, color = Color.White)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete memory", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}
