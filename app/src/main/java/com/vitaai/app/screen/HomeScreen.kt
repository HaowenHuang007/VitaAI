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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.vitaai.app.utils.NutritionCalculator
import com.vitaai.app.utils.NutritionLog
import com.vitaai.app.utils.StreakManager
import com.vitaai.app.utils.WaterManager
import com.vitaai.app.utils.XPManager
import com.vitaai.app.utils.callOpenAIWithHistory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val COLLECTION_USERS = "users"
private const val COLLECTION_MOODS = "moods"
private const val FIELD_XP = "xp"
private const val FIELD_DIET_PLAN = "dietPlan"
private const val FIELD_WEIGHT = "weight"
private const val FIELD_HEIGHT = "height"
private const val FIELD_AGE = "age"
private const val FIELD_ACTIVITY = "activity"
private const val FIELD_GOAL = "goal"
private const val FIELD_MOOD = "mood"
private const val FIELD_RECOMMENDATION = "recommendation"

private const val DATE_FORMAT_ISO = "yyyy-MM-dd"
private const val DATE_FORMAT_DAY = "EEEE"
private const val DATE_FORMAT_DISPLAY = "d MMMM"

private const val DEFAULT_WEIGHT = 70.0
private const val DEFAULT_HEIGHT = 170.0
private const val DEFAULT_AGE = 25
private const val DEFAULT_ACTIVITY = "moderate"
private const val DEFAULT_GOAL = "maintain_weight"

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
    onGoMealPlanner: () -> Unit,
    onGoFavorites: () -> Unit,
    onGoMonthlyStats: () -> Unit,
    onGoVitals: () -> Unit,
    onGoShoppingList: () -> Unit,
    onGoRecipes: () -> Unit,
    onLogout: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val uid = user?.uid ?: return
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var todayMood by remember { mutableStateOf("") }
    var todayMoodId by remember { mutableStateOf("") }
    var todayRecommendation by remember { mutableStateOf("") }
    var dailyTip by remember { mutableStateOf("") }
    var isLoadingTip by remember { mutableStateOf(false) }
    var userXP by remember { mutableIntStateOf(0) }
    var hasPlan by remember { mutableStateOf(false) }

    var streakCurrent by remember { mutableIntStateOf(0) }
    var totalsCalories by remember { mutableIntStateOf(0) }
    var totalsProtein by remember { mutableIntStateOf(0) }
    var totalsCarbs by remember { mutableIntStateOf(0) }
    var totalsFat by remember { mutableIntStateOf(0) }
    var targets by remember { mutableStateOf<NutritionCalculator.DailyTargets?>(null) }
    var waterMl by remember { mutableIntStateOf(0) }

    val today = SimpleDateFormat(DATE_FORMAT_ISO, Locale.US).format(Date())
    val locale = Locale.getDefault()
    
    val dayOfWeek = SimpleDateFormat(DATE_FORMAT_DAY, locale).format(Date())
        .replaceFirstChar { it.uppercase() }
    val dateFormatted = SimpleDateFormat(DATE_FORMAT_DISPLAY, locale).format(Date())
        .replaceFirstChar { it.uppercase() }

    LaunchedEffect(Unit) {
        db.collection(COLLECTION_USERS).document(uid).get()
            .addOnSuccessListener { doc ->
                userXP = (doc.getLong(FIELD_XP) ?: 0).toInt()
                hasPlan = doc.getString(FIELD_DIET_PLAN)?.isNotEmpty() == true
                val w = doc.getDouble(FIELD_WEIGHT) ?: DEFAULT_WEIGHT
                val h = doc.getDouble(FIELD_HEIGHT) ?: DEFAULT_HEIGHT
                val a = (doc.getLong(FIELD_AGE) ?: DEFAULT_AGE).toInt()
                val act = doc.getString(FIELD_ACTIVITY) ?: DEFAULT_ACTIVITY
                val goal = doc.getString(FIELD_GOAL) ?: DEFAULT_GOAL
                targets = NutritionCalculator.computeTargets(w, h, a, act, goal)
            }

        db.collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_MOODS).document(today).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    todayMood = doc.getString(FIELD_MOOD) ?: ""
                    todayMoodId = doc.getString("moodId") ?: ""
                    todayRecommendation = doc.getString(FIELD_RECOMMENDATION) ?: ""
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
                val langName = locale.displayLanguage
                val prompt = """
                    You are a health and nutrition expert.
                    Give ONE brief, practical and motivating health tip for today $dayOfWeek.
                    Maximum 2 sentences.
                    Vary between: nutrition, exercise, rest, hydration, mindfulness.
                    Respond in $langName.
                """.trimIndent()
                dailyTip = callOpenAIWithHistory(prompt, emptyList())
            } catch (_: Exception) {
                dailyTip = "Error loading daily tip"
            }
            isLoadingTip = false
        }
    }

    val levelInfo = XPManager.getLevelInfo(userXP)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = DeepBlue) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(48.dp))
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Column {
                            Box(
                                modifier = Modifier.size(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Gold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconList.Plan,
                                    contentDescription = null,
                                    tint = IconList.PlantGreen,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("VitaAI", fontSize = 22.sp,
                                fontWeight = FontWeight.Bold, color = Gold)
                            Text(user.email ?: "", fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f))
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(IconList.getIconForLevel(levelInfo.level), null, tint = Gold, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${stringResource(R.string.level_label)} ${levelInfo.level} • $userXP XP",
                                    fontSize = 12.sp, color = Gold.copy(alpha = 0.8f))
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(8.dp))

                    DrawerItem(IconList.Plan, stringResource(R.string.home_my_plan)) {
                        scope.launch { drawerState.close() }; onGoQuestionnaire()
                    }
                    DrawerItem(IconList.Goal, stringResource(R.string.home_my_goals)) {
                        scope.launch { drawerState.close() }; onGoGoals()
                    }
                    DrawerItem(IconList.Progress, stringResource(R.string.home_my_progress)) {
                        scope.launch { drawerState.close() }; onGoProgress()
                    }
                    DrawerItem(IconList.WeeklyReport, stringResource(R.string.home_weekly_report)) {
                        scope.launch { drawerState.close() }; onGoWeeklyReport()
                    }
                    DrawerItem(IconList.Chat, stringResource(R.string.home_nutritionist_ia)) {
                        scope.launch { drawerState.close() }; onGoChat()
                    }
                    DrawerItem(IconList.IdentifyFood, stringResource(R.string.home_identify_food)) {
                        scope.launch { drawerState.close() }; onGoCamera()
                    }
                    DrawerItem(IconList.History, stringResource(R.string.home_food_history)) {
                        scope.launch { drawerState.close() }; onGoFoodHistory()
                    }
                    DrawerItem(IconList.Water, stringResource(R.string.home_water_tracker)) {
                        scope.launch { drawerState.close() }; onGoWater()
                    }
                    DrawerItem(IconList.Exercise, stringResource(R.string.home_exercise_log)) {
                        scope.launch { drawerState.close() }; onGoExercise()
                    }
                    DrawerItem(IconList.Achievements, stringResource(R.string.home_achievements)) {
                        scope.launch { drawerState.close() }; onGoAchievements()
                    }
                    DrawerItem(IconList.Level, stringResource(R.string.home_my_level)) {
                        scope.launch { drawerState.close() }; onGoLevel()
                    }
                    DrawerItem(IconList.Settings, stringResource(R.string.home_settings)) {
                        scope.launch { drawerState.close() }; onGoSettings()
                    }
                    DrawerItem(IconList.MealPlanner, stringResource(R.string.home_meal_planner)) {
                        scope.launch { drawerState.close() }; onGoMealPlanner()
                    }
                    DrawerItem(IconList.Recipes, stringResource(R.string.home_recipes)) {
                        scope.launch { drawerState.close() }; onGoRecipes()
                    }
                    DrawerItem(IconList.ShoppingList, stringResource(R.string.home_shopping_list)) {
                        scope.launch { drawerState.close() }; onGoShoppingList()
                    }
                    DrawerItem(IconList.Favorites, stringResource(R.string.home_favorites)) {
                        scope.launch { drawerState.close() }; onGoFavorites()
                    }
                    DrawerItem(IconList.MonthlyStats, stringResource(R.string.home_monthly_stats)) {
                        scope.launch { drawerState.close() }; onGoMonthlyStats()
                    }
                    DrawerItem(IconList.Vitals, stringResource(R.string.home_vitals)) {
                        scope.launch { drawerState.close() }; onGoVitals()
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.common_logout),
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
                        Text(stringResource(R.string.home_good_morning), fontSize = 14.sp,
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
                                Icon(IconList.Fire, null, tint = IconList.FireOrange, modifier = Modifier.size(16.dp))
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
                        Icon(IconList.getIconForLevel(levelInfo.level), null, tint = Gold, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = when(levelInfo.level){
                                5 -> R.string.level_master
                                4 -> R.string.level_expert
                                3 -> R.string.level_athlete
                                2 -> R.string.level_apprentice
                                else -> R.string.level_beginner
                            }), fontSize = 15.sp,
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

                if (targets != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.home_today_nutrition),
                                fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                NutrientRing(IconList.Fire, totalsCalories, targets!!.calories, Gold)
                                NutrientRing(IconList.Protein, totalsProtein, targets!!.proteinG, Color(0xFF42A5F5))
                                NutrientRing(IconList.Carbs, totalsCarbs, targets!!.carbsG, Color(0xFFFFA726))
                                NutrientRing(IconList.Fat, totalsFat, targets!!.fatG, Color(0xFF66BB6A))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

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
                            Icon(IconList.Water, null, tint = IconList.WaterBlue, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.home_water_tracker),
                                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold)
                                Text("$waterMl / ${targets!!.waterMl} ml",
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
                        Icon(IconList.Plan, null, tint = Gold, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.home_my_plan),
                                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gold)
                            Text(
                                if (hasPlan) stringResource(R.string.home_update_plan)
                                else stringResource(R.string.home_generate_plan),
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
                        Text(stringResource(R.string.mood_daily_tip), fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        if (todayMood.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = IconList.getIconForMood(todayMoodId),
                                    contentDescription = null,
                                    tint = IconList.getColorForMood(todayMoodId),
                                    modifier = Modifier.size(36.dp)
                                )
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
                                Icon(IconList.Mood, null, tint = IconList.MoodYellow, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.mood_how_are_you),
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
                        Text(stringResource(R.string.mood_daily_tip), fontSize = 13.sp,
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

                Text(stringResource(R.string.home_quick_access), fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 24.dp))

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickCard(IconList.Camera, stringResource(R.string.home_identify_food), Modifier.weight(1f), onGoCamera)
                    QuickCard(IconList.Chat, stringResource(R.string.home_nutritionist_ia), Modifier.weight(1f), onGoChat)
                    QuickCard(IconList.Exercise, stringResource(R.string.home_exercise_log), Modifier.weight(1f), onGoExercise)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickCard(IconList.Progress, stringResource(R.string.home_my_progress), Modifier.weight(1f), onGoProgress)
                    QuickCard(IconList.History, stringResource(R.string.home_food_history), Modifier.weight(1f), onGoFoodHistory)
                    QuickCard(IconList.Achievements, stringResource(R.string.home_achievements), Modifier.weight(1f), onGoAchievements)
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun NutrientRing(icon: ImageVector, current: Int, target: Int, color: Color) {
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
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("$current", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("/ $target", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
fun DrawerItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
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
fun QuickCard(icon: ImageVector, title: String, modifier: Modifier, onClick: () -> Unit) {
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
            Icon(icon, null, tint = Gold, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, color = Color.White,
                maxLines = 1)
        }
    }
}
