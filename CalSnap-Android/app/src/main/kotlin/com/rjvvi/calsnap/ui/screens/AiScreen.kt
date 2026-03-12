package com.rjvvi.calsnap.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rjvvi.calsnap.viewmodel.AiViewModel
import kotlinx.coroutines.launch

@Composable
fun AiScreen(vm: AiViewModel) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("AI Ассистент", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("🤖 Gemini AI · Спроси про питание", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { vm.clearChat() }) {
                Icon(Icons.Default.Delete, contentDescription = "Очистить чат", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) { Text("🤖", fontSize = 16.sp) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Card(
                        modifier = Modifier.widthIn(max = 280.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(
                            topStart = if (isUser) 18.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 18.dp,
                            bottomStart = 18.dp, bottomEnd = 18.dp
                        ),
                        border = if (!isUser) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null
                    ) {
                        Text(
                            msg.text,
                            modifier = Modifier.padding(12.dp),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                    if (isUser) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) { Text("👤", fontSize = 16.sp) }
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text("🤖", fontSize = 16.sp) }
                        Spacer(Modifier.width(8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)) {
                            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                repeat(3) { i ->
                                    val infTrans = rememberInfiniteTransition(label = "dot$i")
                                    val anim by infTrans.animateFloat(
                                        initialValue = 0.3f, targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(600, delayMillis = i * 150),
                                            repeatMode = RepeatMode.Reverse
                                        ), label = "dot${i}alpha"
                                    )
                                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = anim)))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Error
        error?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(err, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Quick prompts
        if (messages.size == 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prompts = listOf("Сколько калорий в яблоке?", "Что лучше есть утром?", "Как рассчитать TDEE?", "Норма белка в день?")
                items(prompts) { prompt ->
                    SuggestionChip(
                        onClick = { vm.sendMessage(prompt) },
                        label = { Text(prompt, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Input
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Спроси про питание…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val txt = inputText.trim()
                        inputText = ""
                        vm.sendMessage(txt)
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.loweredElevation()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", modifier = Modifier.size(20.dp))
            }
        }
    }
}

