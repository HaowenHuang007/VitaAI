package com.vitaai.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.ui.theme.RoyalBlue
import com.vitaai.app.utils.AchievementManager
import com.vitaai.app.utils.StreakManager
import com.vitaai.app.utils.WaterManager
import com.vitaai.app.utils.XPManager

private const val USERS_COLLECTION = "users"
private const val FIELD_WEIGHT = "weight"
private const val ACTION_WATER_GOAL = "water_goal"
private const val ACHIEVEMENT_FIRST_WATER = "first_water"
private const val ACHIEVEMENT_WATER_GOAL = "water_goal"

@Composable
fun WaterTrackerScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var totalMl by remember { mutableStateOf(0) }
    var targetMl by remember { mutableStateOf(2500) }
    var goalUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection(USERS_COLLECTION).document(uid).get().addOnSuccessListener { doc ->
            val w = doc.getDouble(FIELD_WEIGHT) ?: 70.0
            targetMl = (w * 35).toInt().coerceAtLeast(1500)
        }
        WaterManager.loadToday { totalMl = it }
    }

    fun add(amount: Int) {
        WaterManager.addWater(amount) { newTotal ->
            totalMl = newTotal
            StreakManager.recordActivity()
            if (newTotal >= targetMl && !goalUnlocked) {
                goalUnlocked = true
                AchievementManager.unlock(ACHIEVEMENT_WATER_GOAL)
                XPManager.addXPWithLimit(
                    amount = XPManager.XP_DAILY_LOG,
                    actionKey = ACTION_WATER_GOAL,
                    dailyLimit = 1
                )
            }
            AchievementManager.unlock(ACHIEVEMENT_FIRST_WATER)
        }
    }

    val progress = (totalMl.toFloat() / targetMl.coerceAtLeast(1)).coerceIn(0f, 1.5f)

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(
            imageVector = IconList.Water,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = IconList.WaterBlue
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_water_tracker),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RoyalBlue)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.water_ml_format, totalMl),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold, color = Color.White
                )
                Text(
                    text = stringResource(R.string.water_goal_format, targetMl),
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF42A5F5))
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.water_quick_add),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(200, 300, 500).forEach { ml ->
                Button(
                    onClick = { add(ml) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.water_ml_format, ml),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { WaterManager.resetToday { totalMl = 0; goalUnlocked = false } },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = stringResource(R.string.water_reset_today), fontSize = 14.sp)
        }
    }
}
