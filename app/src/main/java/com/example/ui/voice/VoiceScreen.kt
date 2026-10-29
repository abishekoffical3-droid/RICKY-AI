package com.example.ui.voice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.voice.VoiceState
import com.example.ui.theme.RickyCyan
import com.example.ui.theme.RickyDarkBg
import com.example.ui.theme.RickyPurple
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.voiceManager.stopSpeaking()
        viewModel.voiceManager.stopListening()
        onNavigateBack()
    }

    val voiceState by viewModel.voiceState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Assistant Mode", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.voiceManager.stopSpeaking()
                            viewModel.voiceManager.stopListening()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("voice_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RickyDarkBg,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = RickyDarkBg,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status and Language info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "RICKY AI Hands-Free Voice",
                    color = RickyCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Supports Nepali, English, and Hindi",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            // Central pulsing visualizer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val scale = when (voiceState) {
                    is VoiceState.Listening, is VoiceState.Speaking -> pulseScale
                    else -> 1f
                }
                val glowColor = when (voiceState) {
                    is VoiceState.Listening -> RickyCyan
                    is VoiceState.Speaking -> RickyPurple
                    is VoiceState.Processing -> Color(0xFFFFAB00)
                    else -> Color.DarkGray
                }

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(glowColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(glowColor.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(glowColor)
                                .clickable {
                                    if (voiceState is VoiceState.Speaking) {
                                        viewModel.voiceManager.stopSpeaking()
                                    } else if (voiceState is VoiceState.Listening) {
                                        viewModel.voiceManager.stopListening()
                                    } else {
                                        viewModel.voiceManager.startListening(userProfile.preferredLanguage)
                                    }
                                }
                                .testTag("voice_circle_action"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (voiceState) {
                                    is VoiceState.Listening -> Icons.Default.Mic
                                    is VoiceState.Speaking -> Icons.Default.VolumeUp
                                    is VoiceState.Processing -> Icons.Default.HourglassTop
                                    else -> Icons.Default.MicNone
                                },
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                val stateDescription = when (val state = voiceState) {
                    is VoiceState.Listening -> "Listening... Speak now in Nepali, English, or Hindi"
                    is VoiceState.Processing -> "RICKY is processing your request..."
                    is VoiceState.Speaking -> "Speaking: \"${state.text}\"\n(Tap circle to interrupt)"
                    is VoiceState.Error -> state.message
                    VoiceState.Idle -> "Tap circle to speak with RICKY"
                }

                Text(
                    text = stateDescription,
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Quick action controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.voiceManager.stopSpeaking()
                        viewModel.voiceManager.startListening("ne-NP")
                    },
                    modifier = Modifier.testTag("nepali_voice_btn")
                ) {
                    Text("नेपाली Voice", color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.voiceManager.stopSpeaking()
                        viewModel.voiceManager.startListening("en-US")
                    },
                    modifier = Modifier.testTag("english_voice_btn")
                ) {
                    Text("English Voice", color = Color.White)
                }
            }
        }
    }
}
