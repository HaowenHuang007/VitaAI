package com.vitaai.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.utils.XPManager
import com.vitaai.app.utils.callOpenAIWithHistory
import kotlinx.coroutines.launch
import java.util.Locale

private const val USERS_COLLECTION = "users"
private const val FIELD_GOAL = "goal"
private const val FIELD_WEIGHT = "weight"
private const val FIELD_DIET_PLAN = "dietPlan"

private const val ROLE_USER = "user"
private const val ROLE_ASSISTANT = "assistant"

private const val PROFILE_PREFIX = "Goal: "
private const val PROFILE_WEIGHT = ", Weight: "
private const val PROFILE_UNIT_KG = "kg. Current plan: "

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf("") }

    val langName = Locale.getDefault().displayLanguage

    val welcomeMsg = stringResource(R.string.chat_welcome)

    var messages by remember {
        mutableStateOf(listOf(ChatMessage(welcomeMsg, false)))
    }

    LaunchedEffect(Unit) {
        db.collection(USERS_COLLECTION).document(uid).get()
            .addOnSuccessListener { doc ->
                val goal = doc.getString(FIELD_GOAL) ?: ""
                val weight = doc.getDouble(FIELD_WEIGHT) ?: 0.0
                val dietPlan = doc.getString(FIELD_DIET_PLAN) ?: ""
                userProfile = "$PROFILE_PREFIX$goal$PROFILE_WEIGHT${weight}$PROFILE_UNIT_KG$dietPlan"
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
                placeholder = { Text(stringResource(R.string.chat_ask_me)) },
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
                                - Always introduce yourself as Aira if asked your name
                                USER PROFILE: $userProfile
                                RULES:
                                - Remember EVERYTHING the user has said in this conversation
                                - Give VERY specific advice based on the real profile
                                - ALWAYS respond in $langName
                                - Be concise but useful, max 4 sentences
                                - Use emojis occasionally
                            """.trimIndent()
                            val history = messages.map { msg ->
                                Pair(if (msg.isUser) ROLE_USER else ROLE_ASSISTANT, msg.content)
                            }
                            val response = callOpenAIWithHistory(systemContext, history)
                            messages = messages + ChatMessage(response, false)
                        } catch (e: Exception) {
                            messages = messages + ChatMessage(context.getString(R.string.chat_error), false)
                        }
                        isLoading = false
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = IconList.Send,
                    contentDescription = stringResource(R.string.common_send),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconList.Nutritionist,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = IconList.RobotTeal
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
            ) {
                Icon(
                    imageVector = IconList.Profile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
