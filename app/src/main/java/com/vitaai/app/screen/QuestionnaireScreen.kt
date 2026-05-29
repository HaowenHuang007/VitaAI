package com.vitaai.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.LanguageManager

private object QuestionnaireConstants {
    const val USERS_COLLECTION = "users"
    const val DIET_PLAN_FIELD = "dietPlan"
    const val EXERCISE_PLAN_FIELD = "exercisePlan"
    const val AGE_FIELD = "age"
    const val WEIGHT_FIELD = "weight"
    const val HEIGHT_FIELD = "height"
    const val GOAL_FIELD = "goal"

    const val LANG_LOADING_PLAN = "loading_plan"
    const val LANG_CURRENT_PLAN = "current_plan"
    const val LANG_SAVED_PLAN = "saved_plan"
    const val LANG_DIET_PLAN = "diet_plan"
    const val LANG_EXERCISE_PLAN = "exercise_plan"
    const val LANG_UPDATE_MY_PLAN = "update_my_plan"
    const val LANG_UPDATE_PROFILE = "update_profile"
    const val LANG_PROFILE = "profile"
    const val LANG_TELL_US = "tell_us"
    const val LANG_AGE = "age"
    const val LANG_WEIGHT = "weight"
    const val LANG_HEIGHT = "height"
    const val LANG_GOAL = "goal"
    const val LANG_ACTIVITY_LEVEL = "activity_level"
    const val LANG_UPDATE_PLAN = "update_plan"
    const val LANG_GENERATE_PLAN = "generate_plan"
    const val LANG_SEE_CURRENT_PLAN = "see_current_plan"

    val GOAL_KEYS = listOf("lose_weight", "gain_muscle", "maintain_weight", "improve_health")
    val ACTIVITY_KEYS = listOf("sedentary", "light", "moderate", "active", "very_active")

    const val DECIMAL_REGEX = "^\\d{0,3}([.,]\\d{0,2})?$"
    const val COMMA = ","
    const val DOT = "."
}

@Composable
fun QuestionnaireScreen(
    modifier: Modifier = Modifier,
    onSubmit: (age: Int, weight: Double, height: Double, goal: String, activity: String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var selectedGoal by remember { mutableStateOf("") }
    var selectedActivity by remember { mutableStateOf("") }

    var existingDietPlan by remember { mutableStateOf("") }
    var existingExercisePlan by remember { mutableStateOf("") }
    var hasExistingPlan by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showQuestionnaire by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uid != null) {
            db.collection(QuestionnaireConstants.USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener { doc ->
                    val diet = doc.getString(QuestionnaireConstants.DIET_PLAN_FIELD) ?: ""
                    val exercise = doc.getString(QuestionnaireConstants.EXERCISE_PLAN_FIELD) ?: ""
                    if (diet.isNotEmpty() && exercise.isNotEmpty()) {
                        existingDietPlan = diet
                        existingExercisePlan = exercise
                        hasExistingPlan = true
                        age = (doc.getLong(QuestionnaireConstants.AGE_FIELD) ?: 0).let { if (it == 0L) "" else it.toString() }
                        weight = (doc.getDouble(QuestionnaireConstants.WEIGHT_FIELD) ?: 0.0).let { if (it == 0.0) "" else it.toString() }
                        height = (doc.getDouble(QuestionnaireConstants.HEIGHT_FIELD) ?: 0.0).let { if (it == 0.0) "" else it.toString() }
                        selectedGoal = doc.getString(QuestionnaireConstants.GOAL_FIELD) ?: ""
                    } else {
                        showQuestionnaire = true
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    showQuestionnaire = true
                    isLoading = false
                }
        } else {
            showQuestionnaire = true
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(LanguageManager.t(QuestionnaireConstants.LANG_LOADING_PLAN), color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            hasExistingPlan && !showQuestionnaire -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(LanguageManager.t(QuestionnaireConstants.LANG_CURRENT_PLAN), fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(LanguageManager.t(QuestionnaireConstants.LANG_SAVED_PLAN),
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)

                    Spacer(Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(LanguageManager.t(QuestionnaireConstants.LANG_DIET_PLAN), fontSize = 18.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(existingDietPlan, fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(LanguageManager.t(QuestionnaireConstants.LANG_EXERCISE_PLAN), fontSize = 18.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(existingExercisePlan, fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { showQuestionnaire = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                    ) {
                        Text(LanguageManager.t(QuestionnaireConstants.LANG_UPDATE_MY_PLAN), fontSize = 16.sp, color = Color.White)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))
                    // TODO: This string is primarily an emoji
                    Text("📋", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (hasExistingPlan) LanguageManager.t(QuestionnaireConstants.LANG_UPDATE_PROFILE) else LanguageManager.t(QuestionnaireConstants.LANG_PROFILE),
                        fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(LanguageManager.t(QuestionnaireConstants.LANG_TELL_US), fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline)

                    Spacer(Modifier.height(32.dp))

                    OutlinedTextField(
                        value = age,
                        onValueChange = { new -> if (new.all { it.isDigit() }) age = new },
                        label = { Text(LanguageManager.t(QuestionnaireConstants.LANG_AGE)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { new -> if (new.matches(Regex(QuestionnaireConstants.DECIMAL_REGEX))) weight = new.replace(QuestionnaireConstants.COMMA, QuestionnaireConstants.DOT) },
                        label = { Text(LanguageManager.t(QuestionnaireConstants.LANG_WEIGHT)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = height,
                        onValueChange = { new -> if (new.matches(Regex(QuestionnaireConstants.DECIMAL_REGEX))) height = new.replace(QuestionnaireConstants.COMMA, QuestionnaireConstants.DOT) },
                        label = { Text(LanguageManager.t(QuestionnaireConstants.LANG_HEIGHT)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Spacer(Modifier.height(24.dp))
                    Text(LanguageManager.t(QuestionnaireConstants.LANG_GOAL), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    QuestionnaireConstants.GOAL_KEYS.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { goalKey ->
                                FilterChip(
                                    selected = selectedGoal == goalKey,
                                    onClick = { selectedGoal = goalKey },
                                    label = { Text(LanguageManager.t(goalKey)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(LanguageManager.t(QuestionnaireConstants.LANG_ACTIVITY_LEVEL), fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    QuestionnaireConstants.ACTIVITY_KEYS.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { activityKey ->
                                FilterChip(
                                    selected = selectedActivity == activityKey,
                                    onClick = { selectedActivity = activityKey },
                                    label = { Text(LanguageManager.t(activityKey)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(24.dp))

                    val parsedAge = age.toIntOrNull()
                    val parsedWeight = weight.toDoubleOrNull()
                    val parsedHeight = height.toDoubleOrNull()
                    val isValid = parsedAge != null && parsedAge in 1..120 &&
                            parsedWeight != null && parsedWeight in 20.0..300.0 &&
                            parsedHeight != null && parsedHeight in 50.0..250.0 &&
                            selectedGoal.isNotEmpty() && selectedActivity.isNotEmpty()

                    Button(
                        onClick = {
                            onSubmit(
                                parsedAge!!, parsedWeight!!, parsedHeight!!,
                                LanguageManager.t(selectedGoal),
                                LanguageManager.t(selectedActivity)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isValid
                    ) {
                        Text(
                            if (hasExistingPlan) LanguageManager.t(QuestionnaireConstants.LANG_UPDATE_PLAN)
                            else LanguageManager.t(QuestionnaireConstants.LANG_GENERATE_PLAN),
                            fontSize = 16.sp
                        )
                    }

                    if (hasExistingPlan) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showQuestionnaire = false },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(LanguageManager.t(QuestionnaireConstants.LANG_SEE_CURRENT_PLAN), fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
