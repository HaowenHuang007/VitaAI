package com.vitaai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.screen.*
import com.vitaai.app.ui.theme.VitaAITheme
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.NotificationScheduler
import com.vitaai.app.utils.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        LanguageManager.loadSavedLanguage(this)
        ThemeManager.load(this)
        NotificationScheduler.ensureChannel(this)
        super.onCreate(savedInstanceState)
        setContent {
            VitaAITheme {
                // Observar cambios de idioma para recomponer
                val currentLang by LanguageManager.currentLanguage
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "login") {

                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { navController.navigate("home") { popUpTo("login") { inclusive = true } } },
                            onGoRegister = { navController.navigate("register") }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = { navController.navigate("verify") { popUpTo("login") { inclusive = true } } },
                            onGoLogin = { navController.popBackStack() }
                        )
                    }

                    composable("verify") {
                        VerifyScreen(onGoLogin = {
                            navController.navigate("login") {
                                popUpTo("verify") { inclusive = true }
                            }
                        })
                    }

                    composable("home") {
                        val db = FirebaseFirestore.getInstance()
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        var checkedMood by rememberSaveable { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            if (uid != null && !checkedMood) {
                                val today = java.text.SimpleDateFormat("yyyy-MM-dd",
                                    java.util.Locale.getDefault()).format(java.util.Date())
                                db.collection("users").document(uid)
                                    .collection("moods").document(today)
                                    .get()
                                    .addOnSuccessListener { doc ->
                                        checkedMood = true
                                        if (!doc.exists()) navController.navigate("mood")
                                    }
                            }
                        }
                        HomeScreen(
                            onGoQuestionnaire = { navController.navigate("questionnaire") },
                            onGoProgress = { navController.navigate("progress") },
                            onGoChat = { navController.navigate("chat") },
                            onGoCamera = { navController.navigate("camera") },
                            onGoSettings = { navController.navigate("settings") },
                            onGoLevel = { navController.navigate("level") },
                            onGoWater = { navController.navigate("water") },
                            onGoFoodHistory = { navController.navigate("foodhistory") },
                            onGoGoals = { navController.navigate("goals") },
                            onGoAchievements = { navController.navigate("achievements") },
                            onGoExercise = { navController.navigate("exercise") },
                            onGoWeeklyReport = { navController.navigate("weekly") },
                            onLogout = { navController.navigate("login") { popUpTo("home") { inclusive = true } } }
                        )
                    }

                    composable("questionnaire") {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(LanguageManager.t("my_plan"), color = Color.White) },
                                    navigationIcon = {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Gold)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
                                )
                            }
                        ) { padding ->
                            QuestionnaireScreen(
                                modifier = Modifier.padding(padding),
                                onSubmit = { age, weight, height, goal, activity ->
                                    navController.navigate("plan/$age/$weight/$height/$goal/$activity")
                                }
                            )
                        }
                    }

                    composable("plan/{age}/{weight}/{height}/{goal}/{activity}") { back ->
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(LanguageManager.t("current_plan"), color = Color.White) },
                                    navigationIcon = {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Gold)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
                                )
                            }
                        ) { padding ->
                            PlanScreen(
                                modifier = Modifier.padding(padding),
                                age = back.arguments?.getString("age")?.toInt() ?: 25,
                                weight = back.arguments?.getString("weight")?.toDouble() ?: 70.0,
                                height = back.arguments?.getString("height")?.toDouble() ?: 170.0,
                                goal = back.arguments?.getString("goal") ?: "",
                                activity = back.arguments?.getString("activity") ?: "",
                                onGoHome = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }

                    composable("progress") {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(LanguageManager.t("my_progress"), color = Color.White) },
                                    navigationIcon = {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Gold)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
                                )
                            }
                        ) { padding ->
                            ProgressScreen(modifier = Modifier.padding(padding))
                        }
                    }

                    composable("chat") {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(LanguageManager.t("nutritionist_ia"), color = Color.White) },
                                    navigationIcon = {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Gold)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
                                )
                            }
                        ) { padding ->
                            ChatScreen(modifier = Modifier.padding(padding))
                        }
                    }

                    composable("mood") {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(LanguageManager.t("how_are_you"), color = Color.White) },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
                                )
                            }
                        ) { padding ->
                            MoodScreen(
                                modifier = Modifier.padding(padding),
                                onDone = {
                                    navController.navigate("home") {
                                        popUpTo("mood") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }

                    composable("camera") {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(LanguageManager.t("identify_food"), color = Color.White) },
                                    navigationIcon = {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Gold)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
                                )
                            }
                        ) { padding ->
                            CameraScreen(modifier = Modifier.padding(padding))
                        }
                    }

                    composable("settings") {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(LanguageManager.t("settings"), color = Color.White) },
                                    navigationIcon = {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Gold)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
                                )
                            }
                        ) { padding ->
                            SettingsScreen(
                                modifier = Modifier.padding(padding),
                                onGoBMR = { navController.navigate("bmr") }
                            )
                        }
                    }

                    composable("water") {
                        Scaffold(topBar = { topBarWithBack(navController, LanguageManager.t("water_tracker")) }) { p ->
                            WaterTrackerScreen(modifier = Modifier.padding(p))
                        }
                    }
                    composable("foodhistory") {
                        Scaffold(topBar = { topBarWithBack(navController, LanguageManager.t("food_history")) }) { p ->
                            FoodHistoryScreen(modifier = Modifier.padding(p))
                        }
                    }
                    composable("goals") {
                        Scaffold(topBar = { topBarWithBack(navController, LanguageManager.t("my_goals")) }) { p ->
                            GoalsScreen(modifier = Modifier.padding(p))
                        }
                    }
                    composable("achievements") {
                        Scaffold(topBar = { topBarWithBack(navController, LanguageManager.t("achievements")) }) { p ->
                            AchievementsScreen(modifier = Modifier.padding(p))
                        }
                    }
                    composable("exercise") {
                        Scaffold(topBar = { topBarWithBack(navController, LanguageManager.t("exercise_log")) }) { p ->
                            ExerciseScreen(modifier = Modifier.padding(p))
                        }
                    }
                    composable("weekly") {
                        Scaffold(topBar = { topBarWithBack(navController, LanguageManager.t("weekly_report")) }) { p ->
                            WeeklyReportScreen(modifier = Modifier.padding(p))
                        }
                    }
                    composable("bmr") {
                        Scaffold(topBar = { topBarWithBack(navController, LanguageManager.t("bmr_calculator")) }) { p ->
                            BMRCalculatorScreen(modifier = Modifier.padding(p))
                        }
                    }

                    composable("level") {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text(LanguageManager.t("my_level"), color = Color.White) },
                                    navigationIcon = {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Gold)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
                                )
                            }
                        ) { padding ->
                            LevelScreen(modifier = Modifier.padding(padding))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun topBarWithBack(navController: NavController, title: String) {
    TopAppBar(
        title = { Text(title, color = Color.White) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Gold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
    )
}