package com.vitaai.app.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.ui.theme.DeepBlue
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.NutritionCalculator
import com.vitaai.app.utils.NutritionLog
import com.vitaai.app.utils.StreakManager
import com.vitaai.app.utils.WaterManager
import com.vitaai.app.utils.XPManager
import com.vitaai.app.utils.callOpenAIWithHistory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoQuestionnaire: () -> Unit,
    onGoProgress: () -> Unit,
    onGoChat: () -> Unit,
    onGoCamera: () -> Unit,
    onGoSettings: () -> Unit,
    onGoLevel: () -> Unit,
    onGoWater: () -> Unit,
    onGoFoodHistory: () -> Unit,
    onGoGoals: () -> Unit,
    onGoAchievements: () -> Unit,
    onGoExercise: () -> Unit,
    onGoWeeklyReport: () -> Unit,
    onLogout: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val uid = user?.uid ?: return
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val currentLang by LanguageManager.currentLanguage

    var todayMood by remember { mutableStateOf("") }
    var todayMoodEmoji by remember { mutableStateOf("") }
    var todayRecommendation by remember { mutableStateOf("") }
    var dailyTip by remember { mutableStateOf("") }
    var isLoadingTip by remember { mutableStateOf(false) }
    var userXP by remember { mutableStateOf(0) }
    var hasPlan by remember { mutableStateOf(false) }

    var streakCurrent by remember { mutableStateOf(0) }
    var totalsCalories by remember { mutableStateOf(0) }
    var totalsProtein by remember { mutableStateOf(0) }
    var totalsCarbs by remember { mutableStateOf(0) }
    var totalsFat by remember { mutableStateOf(0) }
    var targets by remember { mutableStateOf<NutritionCalculator.DailyTargets?>(null) }
    var waterMl by remember { mutableStateOf(0) }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val langLocale = when(currentLang) {
        "en" -> Locale.ENGLISH; "fr" -> Locale.FRENCH; "zh" -> Locale.CHINESE
        "pt" -> Locale("pt"); else -> Locale("es")
    }
    val dayOfWeek = SimpleDateFormat("EEEE", langLocale).format(Date())
        .replaceFirstChar { it.uppercase() }
    val dateFormatted = SimpleDateFormat("d MMMM", langLocale).format(Date())
        .replaceFirstChar { it.uppercase() }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userXP = (doc.getLong("xp") ?: 0).toInt()
                hasPlan = doc.getString("dietPlan")?.isNotEmpty() == true
                val w = doc.getDouble("weight") ?: 70.0
                val h = doc.getDouble("height") ?: 170.0
                val a = (doc.getLong("age") ?: 25).toInt()
                val act = doc.getString("activity") ?: "moderate"
                val goal = doc.getString("goal") ?: "maintain_weight"
                targets = NutritionCalculator.computeTargets(w, h, a, act, goal)
            }

        db.collection("users").document(uid)
            .collection("moods").document(today).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    todayMood = doc.getString("mood") ?: ""
                    todayMoodEmoji = doc.getString("emoji") ?: ""
                    todayRecommendation = doc.getString("recommendation") ?: ""
                }
            }

        StreakManager.loadStreak { s -> streakCurrent = s.current }
        NutritionLog.loadToday { t ->
            totalsCalories = t.calories; totalsProtein = t.proteinG
            totalsCarbs = t.carbsG; totalsFat = t.fatG
        }
        WaterManager.loadToday { waterMl = it }

        isLoadingTip = true
        scope.launch {
            try {
                val langName = when(currentLang) {
                    "en" -> "English"; "zh" -> "Chinese"; "fr" -> "French"; "pt" -> "Portuguese"
                    else -> "Spanish"
                }
                val prompt = """
                    You are a health and nutrition expert.
                    Give ONE brief, practical and motivating health tip for today $dayOfWeek.
                    Maximum 2 sentences. Use 1 relevant emoji at the start.
                    Vary between: nutrition, exercise, rest, hydration, mindfulness.
                    Respond in $langName.
                """.trimIndent()
                dailyTip = callOpenAIWithHistory(prompt, emptyList())
            } catch (e: Exception) {
                dailyTip = "💧 ${LanguageManager.t("daily_tip")}"
            }
            isLoadingTip = false
        }
    }

    val levelInfo = XPManager.getLevelInfo(userXP)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = DeepBlue) {
                Spacer(Modifier.height(48.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Column {
                        Box(
                            modifier = Modifier.size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Gold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Text("🌿", fontSize = 30.sp) }
                        Spacer(Modifier.height(8.dp))
                        Text("VitaAI", fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, color = Gold)
                        Text(user.email ?: "", fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f))
                        Spacer(Modifier.height(4.dp))
                        Text("${levelInfo.emoji} ${levelInfo.title} • $userXP XP",
                            fontSize = 12.sp, color = Gold.copy(alpha = 0.8f))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))

                DrawerItem("🤖", LanguageManager.t("my_plan")) {
                    scope.launch { drawerState.close() }; onGoQuestionnaire()
                }
                DrawerItem("🎯", LanguageManager.t("my_goals")) {
                    scope.launch { drawerState.close() }; onGoGoals()
                }
                DrawerItem("📊", LanguageManager.t("my_progress")) {
                    scope.launch { drawerState.close() }; onGoProgress()
                }
                DrawerItem("📈", LanguageManager.t("weekly_report")) {
                    scope.launch { drawerState.close() }; onGoWeeklyReport()
                }
                DrawerItem("💬", LanguageManager.t("nutritionist_ia")) {
                    scope.launch { drawerState.close() }; onGoChat()
                }
                DrawerItem("📸", LanguageManager.t("identify_food")) {
                    scope.launch { drawerState.close() }; onGoCamera()
                }
                DrawerItem("🍽️", LanguageManager.t("food_history")) {
                    scope.launch { drawerState.close() }; onGoFoodHistory()
                }
                DrawerItem("💧", LanguageManager.t("water_tracker")) {
                    scope.launch { drawerState.close() }; onGoWater()
                }
                DrawerItem("🏋️", LanguageManager.t("exercise_log")) {
                    scope.launch { drawerState.close() }; onGoExercise()
                }
                DrawerItem("🏆", LanguageManager.t("achievements")) {
                    scope.launch { drawerState.close() }; onGoAchievements()
                }
                DrawerItem("⭐", LanguageManager.t("my_level")) {
                    scope.launch { drawerState.close() }; onGoLevel()
                }
                DrawerItem("⚙️", LanguageManager.t("settings")) {
                    scope.launch { drawerState.close() }; onGoSettings()
                }

                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                NavigationDrawerItem(
                    label = { Text(LanguageManager.t("logout"),
                        color = Color.White.copy(alpha = 0.6f)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(DeepBlue, NavyBlue)))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(LanguageManager.t("good_morning"), fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f))
                        Text(user.email?.substringBefore("@") ?: "User",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (streakCurrent > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Gold.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("🔥", fontSize = 16.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("$streakCurrent", fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold, color = Gold)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null,
                                tint = Gold, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Text("$dayOfWeek, $dateFormatted", fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 24.dp))

                Spacer(Modifier.height(16.dp))

                // Tarjeta NIVEL/XP
                Card(
                    onClick = onGoLevel,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(levelInfo.emoji, fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(levelInfo.title, fontSize = 15.sp,
                                fontWeight = FontWeight.Bold, color = Gold)
                            Text("$userXP XP", fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f))
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(levelInfo.progress.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Gold)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("›", fontSize = 20.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // === DASHBOARD NUTRICIONAL ===
                if (targets != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(LanguageManager.t("today_nutrition"),
                                fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                NutrientRing("🔥", totalsCalories, targets!!.calories, Gold)
                                NutrientRing("💪", totalsProtein, targets!!.proteinG, Color(0xFF42A5F5))
                                NutrientRing("🍞", totalsCarbs, targets!!.carbsG, Color(0xFFFFA726))
                                NutrientRing("🥑", totalsFat, targets!!.fatG, Color(0xFF66BB6A))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // === WATER CARD ===
                if (targets != null) {
                    Card(
                        onClick = onGoWater,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1976D2).copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💧", fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(LanguageManager.t("water_tracker"),
                                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold)
                                Text("${waterMl} / ${targets!!.waterMl} ml",
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White)
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    val wProgress = (waterMl.toFloat() / targets!!.waterMl)
                                        .coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(wProgress).fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF64B5F6))
                                    )
                                }
                            }
                            Text("›", fontSize = 20.sp, color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // === PLAN CARD ===
                Card(
                    onClick = onGoQuestionnaire,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NavyBlue.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📋", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(LanguageManager.t("my_plan"),
                                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gold)
                            Text(
                                if (hasPlan) LanguageManager.t("update_my_plan")
                                else LanguageManager.t("generate_plan"),
                                fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Text("›", fontSize = 20.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(LanguageManager.t("today_mood"), fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        if (todayMood.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(todayMoodEmoji, fontSize = 36.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(todayMood, fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold, color = Gold)
                                    if (todayRecommendation.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(todayRecommendation.take(100) + "...",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            lineHeight = 18.sp)
                                    }
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("😊", fontSize = 36.sp)
                                Spacer(Modifier.width(12.dp))
                                Text(LanguageManager.t("how_are_you"),
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Gold.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(LanguageManager.t("daily_tip"), fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, color = Gold)
                        Spacer(Modifier.height(8.dp))
                        if (isLoadingTip) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                    color = Gold, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("...", fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.6f))
                            }
                        } else {
                            Text(dailyTip, fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f), lineHeight = 22.sp)
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                Text(LanguageManager.t("quick_access"), fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 24.dp))

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickCard("📸", LanguageManager.t("identify_food"), Modifier.weight(1f), onGoCamera)
                    QuickCard("💬", LanguageManager.t("nutritionist_ia"), Modifier.weight(1f), onGoChat)
                    QuickCard("🏋️", LanguageManager.t("exercise_log"), Modifier.weight(1f), onGoExercise)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickCard("📊", LanguageManager.t("my_progress"), Modifier.weight(1f), onGoProgress)
                    QuickCard("🍽️", LanguageManager.t("food_history"), Modifier.weight(1f), onGoFoodHistory)
                    QuickCard("🏆", LanguageManager.t("achievements"), Modifier.weight(1f), onGoAchievements)
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun NutrientRing(emoji: String, current: Int, target: Int, color: Color) {
    val progress = (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text("$current", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("/ $target", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
fun DrawerItem(emoji: String, title: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Text(title, color = Color.White, fontSize = 15.sp)
            }
        },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
fun QuickCard(emoji: String, title: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, color = Color.White,
                maxLines = 1)
        }
    }
}
