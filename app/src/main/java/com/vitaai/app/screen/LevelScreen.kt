package com.vitaai.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.ui.theme.DeepBlue
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.XPManager

private object LevelConstants {
    const val USERS_COLLECTION = "users"
    const val FIELD_XP = "xp"
    const val FIELD_USERNAME = "username"

    const val LANG_LEVEL = "level"
    const val LANG_XP_TO_NEXT = "xp_to_next"
    const val LANG_MAX_LEVEL_REACHED = "max_level_reached"
    const val LANG_HOW_TO_EARN_XP = "how_to_earn_xp"
    const val LANG_XP_ACTION_PROGRESS = "xp_action_progress"
    const val LANG_XP_ACTION_PLAN = "xp_action_plan"
    const val LANG_XP_ACTION_FOOD = "xp_action_food"
    const val LANG_XP_ACTION_CHAT = "xp_action_chat"
    const val LANG_XP_ACTION_MOOD = "xp_action_mood"
    const val LANG_ALL_LEVELS = "all_levels"
    const val LANG_FROM_XP = "from_xp"
    const val LANG_CURRENT = "current"

    const val TEMPLATE_XP = "{xp}"
    const val UNIT_XP = " XP"
    const val PLUS_PREFIX = "+"
    
    const val LEVEL_BEGINNER = "level_beginner"
    const val LEVEL_APPRENTICE = "level_apprentice"
    const val LEVEL_ATHLETE = "level_athlete"
    const val LEVEL_EXPERT = "level_expert"
    const val LEVEL_MASTER = "level_master"
}

@Composable
fun LevelScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var xp by remember { mutableStateOf(0) }
    var username by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection(LevelConstants.USERS_COLLECTION).document(uid).get()
            .addOnSuccessListener { doc ->
                xp = (doc.getLong(LevelConstants.FIELD_XP) ?: 0).toInt()
                username = doc.getString(LevelConstants.FIELD_USERNAME) ?: ""
            }
    }

    val levelInfo = XPManager.getLevelInfo(xp)

    val levels = listOf(
        // TODO: This string is primarily an emoji
        Triple(1, "🌱", LevelConstants.LEVEL_BEGINNER),
        // TODO: This string is primarily an emoji
        Triple(2, "🌿", LevelConstants.LEVEL_APPRENTICE),
        // TODO: This string is primarily an emoji
        Triple(3, "💪", LevelConstants.LEVEL_ATHLETE),
        // TODO: This string is primarily an emoji
        Triple(4, "⭐", LevelConstants.LEVEL_EXPERT),
        // TODO: This string is primarily an emoji
        Triple(5, "👑", LevelConstants.LEVEL_MASTER)
    )

    val xpThresholds = listOf(0, 100, 300, 600, 1000)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Card nivel actual
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavyBlue)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(levelInfo.emoji, fontSize = 64.sp)
                Spacer(Modifier.height(8.dp))
                Text(levelInfo.title, fontSize = 28.sp,
                    fontWeight = FontWeight.Bold, color = Gold)
                Text("${LanguageManager.t(LevelConstants.LANG_LEVEL)} ${levelInfo.level}", fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(16.dp))

                // XP total
                Text("${levelInfo.currentXP}${LevelConstants.UNIT_XP}", fontSize = 36.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)

                if (levelInfo.level < 5) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        LanguageManager.t(LevelConstants.LANG_XP_TO_NEXT).replace(
                            LevelConstants.TEMPLATE_XP, (levelInfo.xpForNext - levelInfo.currentXP).toString()
                        ),
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))

                    // Barra de progreso
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(levelInfo.progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(Gold)
                        )
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(LanguageManager.t(LevelConstants.LANG_MAX_LEVEL_REACHED),
                        fontSize = 14.sp, color = Gold,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Cómo ganar XP
        // Emoji removed: 💡
        Text(LanguageManager.t(LevelConstants.LANG_HOW_TO_EARN_XP), fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        val xpActions = listOf(
            // TODO: This string is primarily an emoji
            Triple("📊", LanguageManager.t(LevelConstants.LANG_XP_ACTION_PROGRESS), "${LevelConstants.PLUS_PREFIX}${XPManager.XP_DAILY_LOG}${LevelConstants.UNIT_XP}"),
            // TODO: This string is primarily an emoji
            Triple("🤖", LanguageManager.t(LevelConstants.LANG_XP_ACTION_PLAN), "${LevelConstants.PLUS_PREFIX}${XPManager.XP_GENERATE_PLAN}${LevelConstants.UNIT_XP}"),
            // TODO: This string is primarily an emoji
            Triple("📸", LanguageManager.t(LevelConstants.LANG_XP_ACTION_FOOD), "${LevelConstants.PLUS_PREFIX}${XPManager.XP_FOOD_SCAN}${LevelConstants.UNIT_XP}"),
            // TODO: This string is primarily an emoji
            Triple("💬", LanguageManager.t(LevelConstants.LANG_XP_ACTION_CHAT), "${LevelConstants.PLUS_PREFIX}${XPManager.XP_CHAT_MESSAGE}${LevelConstants.UNIT_XP}"),
            // TODO: This string is primarily an emoji
            Triple("😊", LanguageManager.t(LevelConstants.LANG_XP_ACTION_MOOD), "${LevelConstants.PLUS_PREFIX}${XPManager.XP_MOOD_CHECK}${LevelConstants.UNIT_XP}")
        )

        xpActions.forEach { (emoji, action, xpGain) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TODO: This string is primarily an emoji
                    Text(emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(action, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(xpGain, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Todos los niveles
        // Emoji removed: 🏆
        Text(LanguageManager.t(LevelConstants.LANG_ALL_LEVELS), fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        levels.forEach { (level, emoji, titleKey) ->
            val isUnlocked = levelInfo.level >= level
            val isCurrent = levelInfo.level == level
            val xpNeeded = xpThresholds[level - 1]

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCurrent -> NavyBlue
                        isUnlocked -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TODO: The following string contains or is primarily an emoji
                    Text(if (isUnlocked) emoji else "🔒", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            LanguageManager.t(titleKey),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) Gold else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            LanguageManager.t(LevelConstants.LANG_FROM_XP).replace(LevelConstants.TEMPLATE_XP, xpNeeded.toString()),
                            fontSize = 12.sp,
                            color = if (isCurrent) Color.White.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Gold)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(LanguageManager.t(LevelConstants.LANG_CURRENT), fontSize = 10.sp,
                                fontWeight = FontWeight.Bold, color = DeepBlue)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
