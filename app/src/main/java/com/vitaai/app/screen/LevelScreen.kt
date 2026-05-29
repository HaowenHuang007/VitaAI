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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.ui.theme.DeepBlue
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.XPManager

private const val USERS_COLLECTION = "users"
private const val FIELD_XP = "xp"
private const val FIELD_USERNAME = "username"

private const val UNIT_XP = " XP"
private const val PLUS_PREFIX = "+"

@Composable
fun LevelScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var xp by remember { mutableStateOf(0) }
    var username by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection(USERS_COLLECTION).document(uid).get()
            .addOnSuccessListener { doc ->
                xp = (doc.getLong(FIELD_XP) ?: 0).toInt()
                username = doc.getString(FIELD_USERNAME) ?: ""
            }
    }

    val levelInfo = XPManager.getLevelInfo(xp)

    val levels = listOf(
        Triple(1, R.string.level_beginner, 0),
        Triple(2, R.string.level_apprentice, 100),
        Triple(3, R.string.level_athlete, 300),
        Triple(4, R.string.level_expert, 600),
        Triple(5, R.string.level_master, 1000)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavyBlue)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = IconList.getIconForLevel(levelInfo.level),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Gold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(id = when(levelInfo.level) {
                        5 -> R.string.level_master
                        4 -> R.string.level_expert
                        3 -> R.string.level_athlete
                        2 -> R.string.level_apprentice
                        else -> R.string.level_beginner
                    }),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                Text(
                    text = "${stringResource(R.string.level_label)} ${levelInfo.level}",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "${levelInfo.currentXP}$UNIT_XP",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (levelInfo.level < 5) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.level_xp_to_next, levelInfo.xpForNext - levelInfo.currentXP),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))

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
                    Text(
                        text = stringResource(R.string.level_max_reached),
                        fontSize = 14.sp,
                        color = Gold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.level_how_to_earn),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))

        val xpActions = listOf(
            Triple(IconList.Progress, R.string.level_xp_action_progress, XPManager.XP_DAILY_LOG),
            Triple(IconList.Plan, R.string.level_xp_action_plan, XPManager.XP_GENERATE_PLAN),
            Triple(IconList.Camera, R.string.level_xp_action_food, XPManager.XP_FOOD_SCAN),
            Triple(IconList.Chat, R.string.level_xp_action_chat, XPManager.XP_CHAT_MESSAGE),
            Triple(IconList.Mood, R.string.level_xp_action_mood, XPManager.XP_MOOD_CHECK)
        )

        xpActions.forEach { (icon, actionRes, xpGain) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(actionRes),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$PLUS_PREFIX$xpGain$UNIT_XP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.level_all_levels),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))

        levels.forEach { (level, titleResId, xpNeeded) ->
            val isUnlocked = levelInfo.level >= level
            val isCurrent = levelInfo.level == level

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
                    Icon(
                        imageVector = if (isUnlocked) IconList.getIconForLevel(level) else IconList.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (isCurrent) Gold else if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(titleResId),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) Gold else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.level_from_xp, xpNeeded),
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
                            Text(
                                text = stringResource(R.string.level_current_tag),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
