package uz.sentinel.presentation.aiagent

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: AiAssistantViewModel,
    onExecuteAction: (AiActionTarget) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "🔍 Telefonimni tekshir va tavsiya ber",
        "🎮 O'yin rejimi va RAM tezlashtirish",
        "🦠 Telegram APK viruslardan himoya",
        "🔒 AppLock va Sozlamalarni qulflash",
        "🌐 Internetsiz (Oflayn) qanday ishlaydi?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // AI Header Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = ObsidianSurface,
            borderColor = GlassBorderTeal,
            borderWidth = 1.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = CyberTeal.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyberTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sentinel AI Agent",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.isOfflineOnlyMode || !uiState.isOnlineAvailable)
                                "🔵 Oflayn Avtonom AI (Internetsiz tayyor)"
                            else
                                "🟢 Onlayn + Lokal Gibrid AI Maslahatchi",
                            fontSize = 11.sp,
                            color = CyberTeal
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { viewModel.runSmartDeviceDiagnosis(isInitialGreeting = false) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tahlil", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Prompt Chips (Horizontal Scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { prompt ->
                AssistChip(
                    onClick = { viewModel.askQuestion(prompt) },
                    label = { Text(prompt, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = ObsidianSurfaceElev,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.messages, key = { it.id }) { msg ->
                AiMessageBubble(
                    message = msg,
                    onActionClick = onExecuteAction
                )
            }
            if (uiState.isAnalyzing) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = CyberTeal,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI qurilmani tahlil qilmoqda...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Savol bering yoki tavsiya so'rang...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.askQuestion(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(CyberTeal, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Yuborish",
                    tint = ObsidianBlack
                )
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiChatMessage,
    onActionClick: (AiActionTarget) -> Unit
) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isFromUser) CyberTealDark else ObsidianSurfaceElev
    val borderColor = if (message.isFromUser) CyberTeal.copy(alpha = 0.4f) else GlassBorderWhite

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            ),
            color = bgColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text.replace("**", ""),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (message.recommendedAction != null && !message.isFromUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onActionClick(message.recommendedAction) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberTeal,
                            contentColor = ObsidianBlack
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = message.recommendedAction.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
