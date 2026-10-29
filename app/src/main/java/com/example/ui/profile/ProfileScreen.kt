package com.example.ui.profile

import android.widget.Toast
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
import com.example.core.identity.AnswerStyle
import com.example.core.identity.UserProfile
import com.example.ui.theme.RickyCyan
import com.example.ui.theme.RickyDarkSurfaceVariant
import com.example.ui.theme.RickyPurple
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()

    var name by remember(userProfile) { mutableStateOf(userProfile.displayName) }
    var region by remember(userProfile) { mutableStateOf(userProfile.region) }
    var language by remember(userProfile) { mutableStateOf(userProfile.preferredLanguage) }
    var interestsStr by remember(userProfile) { mutableStateOf(userProfile.interests.joinToString(", ")) }
    var projects by remember(userProfile) { mutableStateOf(userProfile.projects) }
    var answerStyle by remember(userProfile) { mutableStateOf(userProfile.answerStyle) }
    var voiceSpeed by remember(userProfile) { mutableFloatStateOf(userProfile.voiceSpeed) }
    var voicePitch by remember(userProfile) { mutableFloatStateOf(userProfile.voicePitch) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile & Preferences", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("profile_back_button")) {
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
            // Header Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(RickyPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (name.isNotEmpty()) name.first().uppercase() else "A",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Personal Assistant Context", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text("Personal Details (Editable Starter Values)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RickyCyan)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = region,
                onValueChange = { region = it },
                label = { Text("General Region / Country") },
                modifier = Modifier.fillMaxWidth().testTag("profile_region_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text("Preferred Language (Nepali / English / Hindi)") },
                modifier = Modifier.fillMaxWidth().testTag("profile_lang_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = interestsStr,
                onValueChange = { interestsStr = it },
                label = { Text("Interests & Hobbies (comma-separated)") },
                modifier = Modifier.fillMaxWidth().testTag("profile_interests_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = projects,
                onValueChange = { projects = it },
                label = { Text("Current Projects & Goals") },
                modifier = Modifier.fillMaxWidth().testTag("profile_projects_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Answer Style Preference", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RickyCyan)
            Spacer(modifier = Modifier.height(6.dp))

            AnswerStyle.values().forEach { style ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = answerStyle == style,
                        onClick = { answerStyle = style },
                        colors = RadioButtonDefaults.colors(selectedColor = RickyCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(style.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(style.description, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Voice Synthesis Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RickyCyan)
            Text("Voice Speed: ${"%.1f".format(voiceSpeed)}x", fontSize = 12.sp)
            Slider(
                value = voiceSpeed,
                onValueChange = { voiceSpeed = it },
                valueRange = 0.5f..2.0f,
                steps = 15,
                colors = SliderDefaults.colors(thumbColor = RickyCyan, activeTrackColor = RickyCyan)
            )

            Text("Voice Pitch: ${"%.1f".format(voicePitch)}x", fontSize = 12.sp)
            Slider(
                value = voicePitch,
                onValueChange = { voicePitch = it },
                valueRange = 0.5f..2.0f,
                steps = 15,
                colors = SliderDefaults.colors(thumbColor = RickyPurple, activeTrackColor = RickyPurple)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val updatedProfile = UserProfile(
                        displayName = name.trim(),
                        region = region.trim(),
                        preferredLanguage = language.trim(),
                        interests = interestsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        projects = projects.trim(),
                        answerStyle = answerStyle,
                        voiceSpeed = voiceSpeed,
                        voicePitch = voicePitch,
                        memoryEnabled = userProfile.memoryEnabled
                    )
                    coroutineScope.launch {
                        viewModel.preferencesManager.saveUserProfile(updatedProfile)
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("save_profile_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = RickyCyan, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        }
    }
}
