package com.vitaai.app.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.XPManager
import com.vitaai.app.utils.callOpenAIWithHistory
import kotlinx.coroutines.launch

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf("") }

    val langName = when(LanguageManager.currentLanguage.value) {
        "en" -> "English"
        "zh" -> "Chinese"
        "fr" -> "French"
        "pt" -> "Portuguese"
        else -> "Spanish"
    }

    val welcomeMsg = when(LanguageManager.currentLanguage.value) {
        "en" -> "Hi! I'm Aira, your personal AI nutritionist 🥗 I can help with food advice, analyze your meals and adjust your plan. How can I help you today?"
        "zh" -> "你好！我是Aira，你的AI私人营养师 🥗 我可以帮你提供饮食建议、分析你的饮食并调整你的计划。今天需要什么帮助？"
        "fr" -> "Bonjour! Je suis Aira, votre nutritionniste IA personnelle 🥗 Je peux vous aider avec des conseils alimentaires. Comment puis-je vous aider?"
        "pt" -> "Olá! Sou Aira, sua nutricionista IA pessoal 🥗 Posso ajudá-lo com conselhos alimentares. Como posso ajudá-lo hoje?"
        else -> "¡Hola! Soy Aira, tu nutricionista personal IA 🥗 Puedo ayudarte con consejos de alimentación, analizar tus comidas y ajustar tu plan. ¿En qué te ayudo hoy?"
    }

    var messages by remember {
        mutableStateOf(listOf(ChatMessage(welcomeMsg, false)))
    }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val goal = doc.getString("goal") ?: ""
                val weight = doc.getDouble("weight") ?: 0.0
                val dietPlan = doc.getString("dietPlan") ?: ""
                userProfile = "Goal: $goal, Weight: ${weight}kg. Current plan: $dietPlan"
            }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { msg -> ChatBubble(msg) }
            if (isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp, 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userInput, onValueChange = { userInput = it },
                placeholder = { Text(LanguageManager.t("ask_me")) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            FloatingActionButton(
                onClick = {
                    if (userInput.isBlank() || isLoading) return@FloatingActionButton
                    val question = userInput.trim()
                    userInput = ""
                    messages = messages + ChatMessage(question, true)
                    isLoading = true
                    XPManager.addXPWithLimit(
                        amount = XPManager.XP_CHAT_MESSAGE,
                        actionKey = XPManager.KEY_CHAT,
                        dailyLimit = 5
                    )
                    scope.launch {
                        try {
                            val systemContext = """
                                You are Aira, an expert AI nutritionist and personal trainer, empathetic and motivating. Your name is Aira.
                                USER PROFILE: $userProfile
                                RULES:
                                - Remember EVERYTHING the user has said in this conversation
                                - Give VERY specific advice based on the real profile
                                - ALWAYS respond in $langName
                                - Be concise but useful, max 4 sentences
                                - Use emojis occasionally
                                - Always introduce yourself as Aira if asked
                            """.trimIndent()
                            val history = messages.map { msg ->
                                Pair(if (msg.isUser) "user" else "assistant", msg.content)
                            }
                            val response = callOpenAIWithHistory(systemContext, history)
                            messages = messages + ChatMessage(response, false)
                        } catch (e: Exception) {
                            messages = messages + ChatMessage(LanguageManager.t("chat_error"), false)
                        }
                        isLoading = false
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = LanguageManager.t("send"),
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF010721)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_mark),
                    contentDescription = "VitaAI",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    if (msg.isUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                    else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                )
                .background(
                    if (msg.isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp, 8.dp)
        ) {
            Text(msg.content, fontSize = 14.sp,
                color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp)
        }
        if (msg.isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 16.sp) }
        }
    }
}

