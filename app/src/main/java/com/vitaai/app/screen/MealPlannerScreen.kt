package com.vitaai.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.NutritionCalculator
import com.vitaai.app.utils.NutritionLog
import com.vitaai.app.utils.callOpenAI
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

private data class MealSuggestion(
    val name: String,
    val description: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

@Composable
fun MealPlannerScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var breakfast by remember { mutableStateOf<MealSuggestion?>(null) }
    var lunch by remember { mutableStateOf<MealSuggestion?>(null) }
    var dinner by remember { mutableStateOf<MealSuggestion?>(null) }
    var snack by remember { mutableStateOf<MealSuggestion?>(null) }
    var totals by remember { mutableStateOf<NutritionLog.DailyTotals?>(null) }
    var targets by remember { mutableStateOf<NutritionCalculator.DailyTargets?>(null) }

    val langName = when (LanguageManager.currentLanguage.value) {
        "en" -> "English"; "zh" -> "Chinese"; "fr" -> "French"; "pt" -> "Portuguese"
        else -> "Spanish"
    }

    suspend fun generate() {
        val doc = db.collection("users").document(uid).get().await()
        val w = doc.getDouble("weight") ?: 70.0
        val h = doc.getDouble("height") ?: 170.0
        val a = (doc.getLong("age") ?: 25).toInt()
        val act = doc.getString("activity") ?: "moderate"
        val goal = doc.getString("goal") ?: "maintain_weight"
        val t = NutritionCalculator.computeTargets(w, h, a, act, goal)
        targets = t
        val nutritionDoc = try {
            db.collection("users").document(uid)
                .collection("nutrition").document(com.vitaai.app.utils.DateUtils.todayKey())
                .get().await()
        } catch (e: Exception) { null }
        val current = NutritionLog.DailyTotals(
            calories = (nutritionDoc?.getLong("calories") ?: 0).toInt(),
            proteinG = (nutritionDoc?.getLong("proteinG") ?: 0).toInt(),
            carbsG = (nutritionDoc?.getLong("carbsG") ?: 0).toInt(),
            fatG = (nutritionDoc?.getLong("fatG") ?: 0).toInt()
        )
        totals = current

        val remCal = (t.calories - current.calories).coerceAtLeast(400)
        val remProtein = (t.proteinG - current.proteinG).coerceAtLeast(20)
        val remCarbs = (t.carbsG - current.carbsG).coerceAtLeast(50)
        val remFat = (t.fatG - current.fatG).coerceAtLeast(15)

        val prompt = """
            You are an expert nutritionist. Generate a meal plan for the rest of today.
            Respond in $langName. Return strict JSON, no markdown:
            {
              "breakfast": { "name": "...", "description": "ingredients + brief recipe", "calories": N, "protein": N, "carbs": N, "fat": N },
              "lunch":     { same shape },
              "dinner":    { same shape },
              "snack":     { same shape }
            }
            REMAINING TODAY: $remCal kcal, ${remProtein}g protein, ${remCarbs}g carbs, ${remFat}g fat.
            Goal: $goal. Make meals realistic, balanced, and easy to prepare.
            Sum of all 4 meals must be close to remaining macros above.
        """.trimIndent()

        val raw = callOpenAI(prompt)
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        fun parse(key: String): MealSuggestion {
            val o = json.getJSONObject(key)
            return MealSuggestion(
                name = o.optString("name"),
                description = o.optString("description"),
                calories = o.optInt("calories"),
                proteinG = o.optInt("protein"),
                carbsG = o.optInt("carbs"),
                fatG = o.optInt("fat")
            )
        }
        breakfast = parse("breakfast")
        lunch = parse("lunch")
        dinner = parse("dinner")
        snack = parse("snack")
    }

    LaunchedEffect(Unit) {
        isLoading = true
        try { generate() } catch (e: Exception) {
            errorMsg = LanguageManager.t("chat_error") + ": ${e.message}"
        }
        isLoading = false
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("🍳 ${LanguageManager.t("meal_planner")}", fontSize = 24.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(LanguageManager.t("meal_planner_subtitle"),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(top = 64.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(LanguageManager.t("generating_meals"),
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        } else if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error)
        } else {
            listOf(
                "🌅" to ("breakfast" to breakfast),
                "🌞" to ("lunch" to lunch),
                "🌙" to ("dinner" to dinner),
                "🍪" to ("snack" to snack)
            ).forEach { (emoji, pair) ->
                val (key, meal) = pair
                if (meal != null) {
                    MealCard(emoji, LanguageManager.t("meal_$key"), meal) {
                        // log this meal
                        db.collection("users").document(uid).collection("foodlog")
                            .add(mapOf(
                                "date" to com.vitaai.app.utils.DateUtils.todayKey(),
                                "name" to meal.name,
                                "calories" to meal.calories,
                                "proteinG" to meal.proteinG,
                                "carbsG" to meal.carbsG,
                                "fatG" to meal.fatG,
                                "rating" to "healthy",
                                "fromMealPlan" to true,
                                "timestamp" to System.currentTimeMillis()
                            ))
                        NutritionLog.addMeal(meal.calories, meal.proteinG, meal.carbsG, meal.fatG)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    isLoading = true
                    breakfast = null; lunch = null; dinner = null; snack = null
                    errorMsg = ""
                    scope.launch {
                        try { generate() } catch (e: Exception) {
                            errorMsg = LanguageManager.t("chat_error") + ": ${e.message}"
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(LanguageManager.t("regenerate"), fontSize = 14.sp) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MealCard(
    emoji: String,
    title: String,
    meal: MealSuggestion,
    onLog: () -> Unit
) {
    var logged by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 28.sp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.SemiBold)
                    Text(meal.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("${meal.calories} kcal", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(meal.description, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "💪 ${meal.proteinG}g · 🍞 ${meal.carbsG}g · 🥑 ${meal.fatG}g",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onLog(); logged = true },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !logged
            ) {
                Text(
                    if (logged) "✅ ${LanguageManager.t("saved")}"
                    else "✅ ${LanguageManager.t("log_meal")}",
                    fontSize = 13.sp
                )
            }
        }
    }
}
