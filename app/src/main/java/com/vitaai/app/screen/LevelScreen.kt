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

@Composable
fun LevelScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var xp by remember { mutableStateOf(0) }
    var username by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                xp = (doc.getLong("xp") ?: 0).toInt()
                username = doc.getString("username") ?: ""
            }
    }

    val levelInfo = XPManager.getLevelInfo(xp)

    val levels = listOf(
        Triple(1, "🌱", "level_beginner"),
        Triple(2, "🌿", "level_apprentice"),
        Triple(3, "💪", "level_athlete"),
        Triple(4, "⭐", "level_expert"),
        Triple(5, "👑", "level_master")
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
                Text("${LanguageManager.t("level")} ${levelInfo.level}", fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(16.dp))

                // XP total
                Text("${levelInfo.currentXP} XP", fontSize = 36.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)

                if (levelInfo.level < 5) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        LanguageManager.t("xp_to_next").replace(
                            "{xp}", (levelInfo.xpForNext - levelInfo.currentXP).toString()
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
                    Text(LanguageManager.t("max_level_reached"),
                        fontSize = 14.sp, color = Gold,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Cómo ganar XP
        Text("💡 ${LanguageManager.t("how_to_earn_xp")}", fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        val xpActions = listOf(
            Triple("📊", LanguageManager.t("xp_action_progress"), "+${XPManager.XP_DAILY_LOG} XP"),
            Triple("🤖", LanguageManager.t("xp_action_plan"), "+${XPManager.XP_GENERATE_PLAN} XP"),
            Triple("📸", LanguageManager.t("xp_action_food"), "+${XPManager.XP_FOOD_SCAN} XP"),
            Triple("💬", LanguageManager.t("xp_action_chat"), "+${XPManager.XP_CHAT_MESSAGE} XP"),
            Triple("😊", LanguageManager.t("xp_action_mood"), "+${XPManager.XP_MOOD_CHECK} XP")
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
        Text("🏆 ${LanguageManager.t("all_levels")}", fontSize = 18.sp,
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
                            LanguageManager.t("from_xp").replace("{xp}", xpNeeded.toString()),
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
                            Text(LanguageManager.t("current"), fontSize = 10.sp,
                                fontWeight = FontWeight.Bold, color = DeepBlue)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}