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

@Composable
fun BMRCalculatorScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var weight by remember { mutableStateOf(0.0) }
    var height by remember { mutableStateOf(0.0) }
    var age by remember { mutableStateOf(0) }
    var activity by remember { mutableStateOf("moderate") }
    var goal by remember { mutableStateOf("maintain_weight") }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener {
            weight = it.getDouble("weight") ?: 70.0
            height = it.getDouble("height") ?: 170.0
            age = (it.getLong("age") ?: 25).toInt()
            activity = it.getString("activity") ?: "moderate"
            goal = it.getString("goal") ?: "maintain_weight"
            ready = true
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("🧮 ${LanguageManager.t("bmr_calculator")}",
            fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        if (!ready) {
            CircularProgressIndicator()
            return@Column
        }

        val targets = NutritionCalculator.computeTargets(weight, height, age, activity, goal)
        val bmi = NutritionCalculator.bmi(weight, height)
        val bmiCatKey = NutritionCalculator.bmiCategoryKey(bmi)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("⚖️ ${LanguageManager.t("bmi")}: ${"%.1f".format(bmi)}",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(LanguageManager.t(bmiCatKey),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🔥 BMR", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline)
                        Text("${targets.bmr} kcal", fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text(LanguageManager.t("bmr_explain"),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("⚡ TDEE", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline)
                        Text("${targets.tdee} kcal", fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text(LanguageManager.t("tdee_explain"),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🎯 ${LanguageManager.t("recommended_targets")}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                listOf(
                    "🔥 ${LanguageManager.t("calories")}" to "${targets.calories} kcal",
                    "💪 ${LanguageManager.t("protein")}" to "${targets.proteinG} g",
                    "🍞 ${LanguageManager.t("carbs")}" to "${targets.carbsG} g",
                    "🥑 ${LanguageManager.t("fat")}" to "${targets.fatG} g",
                    "💧 ${LanguageManager.t("water")}" to "${targets.waterMl} ml"
                ).forEach { (k, v) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(k, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(v, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
