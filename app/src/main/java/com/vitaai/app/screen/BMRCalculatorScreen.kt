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

private object BmrConstants {
    const val USERS_COLLECTION = "users"
    const val WEIGHT_FIELD = "weight"
    const val HEIGHT_FIELD = "height"
    const val AGE_FIELD = "age"
    const val ACTIVITY_FIELD = "activity"
    const val GOAL_FIELD = "goal"
    const val DEFAULT_ACTIVITY = "moderate"
    const val DEFAULT_GOAL = "maintain_weight"
    const val BMR_CALCULATOR_LANG = "bmr_calculator"
    const val BMI_LANG = "bmi"
    const val BMR_EXPLAIN_LANG = "bmr_explain"
    const val TDEE_EXPLAIN_LANG = "tdee_explain"
    const val RECOMMENDED_TARGETS_LANG = "recommended_targets"
    const val CALORIES_LANG = "calories"
    const val PROTEIN_LANG = "protein"
    const val CARBS_LANG = "carbs"
    const val FAT_LANG = "fat"
    const val WATER_LANG = "water"
    const val UNIT_KCAL = " kcal"
    const val UNIT_G = " g"
    const val UNIT_ML = " ml"
    const val BMI_FORMAT = "%.1f"
}

@Composable
fun BMRCalculatorScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var weight by remember { mutableStateOf(0.0) }
    var height by remember { mutableStateOf(0.0) }
    var age by remember { mutableStateOf(0) }
    var activity by remember { mutableStateOf(BmrConstants.DEFAULT_ACTIVITY) }
    var goal by remember { mutableStateOf(BmrConstants.DEFAULT_GOAL) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection(BmrConstants.USERS_COLLECTION).document(uid).get().addOnSuccessListener {
            weight = it.getDouble(BmrConstants.WEIGHT_FIELD) ?: 70.0
            height = it.getDouble(BmrConstants.HEIGHT_FIELD) ?: 170.0
            age = (it.getLong(BmrConstants.AGE_FIELD) ?: 25).toInt()
            activity = it.getString(BmrConstants.ACTIVITY_FIELD) ?: BmrConstants.DEFAULT_ACTIVITY
            goal = it.getString(BmrConstants.GOAL_FIELD) ?: BmrConstants.DEFAULT_GOAL
            ready = true
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        // Emoji removed: 🧮
        Text(LanguageManager.t(BmrConstants.BMR_CALCULATOR_LANG),
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
                // Emoji removed: ⚖️
                Text("${LanguageManager.t(BmrConstants.BMI_LANG)}: ${BmrConstants.BMI_FORMAT.format(bmi)}",
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
                        // Emoji removed: 🔥
                        Text("BMR", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline)
                        Text("${targets.bmr}${BmrConstants.UNIT_KCAL}", fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text(LanguageManager.t(BmrConstants.BMR_EXPLAIN_LANG),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        // Emoji removed: ⚡
                        Text("TDEE", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline)
                        Text("${targets.tdee}${BmrConstants.UNIT_KCAL}", fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text(LanguageManager.t(BmrConstants.TDEE_EXPLAIN_LANG),
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
                // Emoji removed: 🎯
                Text(LanguageManager.t(BmrConstants.RECOMMENDED_TARGETS_LANG),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                listOf(
                    // Emoji removed: 🔥
                    LanguageManager.t(BmrConstants.CALORIES_LANG) to "${targets.calories}${BmrConstants.UNIT_KCAL}",
                    // Emoji removed: 💪
                    LanguageManager.t(BmrConstants.PROTEIN_LANG) to "${targets.proteinG}${BmrConstants.UNIT_G}",
                    // Emoji removed: 🍞
                    LanguageManager.t(BmrConstants.CARBS_LANG) to "${targets.carbsG}${BmrConstants.UNIT_G}",
                    // Emoji removed: 🥑
                    LanguageManager.t(BmrConstants.FAT_LANG) to "${targets.fatG}${BmrConstants.UNIT_G}",
                    // Emoji removed: 💧
                    LanguageManager.t(BmrConstants.WATER_LANG) to "${targets.waterMl}${BmrConstants.UNIT_ML}"
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
