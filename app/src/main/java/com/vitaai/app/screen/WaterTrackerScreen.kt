package com.vitaai.app.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.ui.theme.RoyalBlue
import com.vitaai.app.utils.AchievementManager
import com.vitaai.app.utils.DateUtils
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.NutritionCalculator
import com.vitaai.app.utils.StreakManager
import com.vitaai.app.utils.WaterManager
import com.vitaai.app.utils.XPManager

@Composable
fun WaterTrackerScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var totalMl by remember { mutableStateOf(0) }
    var targetMl by remember { mutableStateOf(2500) }
    var goalUnlocked by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var saveError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val w = doc.getDouble("weight") ?: 70.0
            targetMl = (w * 35).toInt().coerceAtLeast(1500)
        }
        WaterManager.loadToday { totalMl = it }
        WaterManager.loadHistory(7) { history = it }
    }

    fun add(amount: Int) {
        WaterManager.addWater(amount) { newTotal ->
            if (newTotal == -1) { saveError = LanguageManager.t("error_saving"); return@addWater }
            saveError = ""
            totalMl = newTotal
            StreakManager.recordActivity()
            if (newTotal >= targetMl && !goalUnlocked) {
                goalUnlocked = true
                AchievementManager.unlock("water_goal")
                XPManager.addXPWithLimit(
                    amount = XPManager.XP_DAILY_LOG,
                    actionKey = "water_goal",
                    dailyLimit = 1
                )
            }
            AchievementManager.unlock("first_water")
            WaterManager.loadHistory(7) { history = it }
        }
    }

    val progress = (totalMl.toFloat() / targetMl.coerceAtLeast(1)).coerceIn(0f, 1.5f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "waterProgress"
    )
    val animatedTotal by animateIntAsState(
        targetValue = totalMl,
        animationSpec = tween(durationMillis = 900),
        label = "waterTotal"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("💧", fontSize = 64.sp)
        Spacer(Modifier.height(8.dp))
        Text(LanguageManager.t("water_tracker"), fontSize = 24.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        if (saveError.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(saveError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

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
                Text("${animatedTotal} ml", fontSize = 48.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(LanguageManager.t("goal") + " ${targetMl} ml",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF42A5F5))
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("${(animatedProgress * 100).toInt()}%",
                    fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(LanguageManager.t("quick_add"), fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
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
                    Text("+${ml}ml", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                WaterManager.resetToday {
                    totalMl = 0
                    goalUnlocked = false
                    WaterManager.loadHistory(7) { history = it }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(LanguageManager.t("reset_today"), fontSize = 14.sp)
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                LanguageManager.t("recent_history"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            history.forEach { (date, ml) ->
                val label = when {
                    date == DateUtils.todayKey() -> LanguageManager.t("today")
                    DateUtils.isYesterday(date) -> LanguageManager.t("yesterday")
                    else -> date
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💧", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    val pct = (ml.toFloat() / targetMl.coerceAtLeast(1) * 100).toInt()
                    Text("${ml} ml  ($pct%)", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (ml >= targetMl) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
