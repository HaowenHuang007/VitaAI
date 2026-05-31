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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.ui.theme.NavyBlue

private const val USERS_COLLECTION = "users"
private const val DIET_PLAN_FIELD = "dietPlan"
private const val EXERCISE_PLAN_FIELD = "exercisePlan"
private const val AGE_FIELD = "age"
private const val WEIGHT_FIELD = "weight"
private const val HEIGHT_FIELD = "height"
private const val GOAL_FIELD = "goal"

private val GOAL_OPTIONS = listOf(
    "lose_weight" to R.string.goal_lose_weight,
    "gain_muscle" to R.string.goal_gain_muscle,
    "maintain_weight" to R.string.goal_maintain_weight,
    "improve_health" to R.string.goal_improve_health
)

private val ACTIVITY_OPTIONS = listOf(
    "sedentary" to R.string.act_sedentary,
    "light" to R.string.act_light,
    "moderate" to R.string.act_moderate,
    "active" to R.string.act_active,
    "very_active" to R.string.act_very_active
)

@Composable
fun QuestionnaireScreen(
    modifier: Modifier = Modifier,
    onSubmit: (age: Int, weight: Double, height: Double, goal: String, activity: String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var ageInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var selectedGoalKey by remember { mutableStateOf("") }
    var selectedActivityKey by remember { mutableStateOf("") }

    var existingDietPlan by remember { mutableStateOf("") }
    var existingExercisePlan by remember { mutableStateOf("") }
    var hasExistingPlan by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showQuestionnaire by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uid != null) {
            db.collection(USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener { doc ->
                    val diet = doc.getString(DIET_PLAN_FIELD) ?: ""
                    val exercise = doc.getString(EXERCISE_PLAN_FIELD) ?: ""
                    if (diet.isNotEmpty() && exercise.isNotEmpty()) {
                        existingDietPlan = diet
                        existingExercisePlan = exercise
                        hasExistingPlan = true
                        ageInput = (doc.getLong(AGE_FIELD) ?: 0).let { if (it == 0L) "" else it.toString() }
                        weightInput = (doc.getDouble(WEIGHT_FIELD) ?: 0.0).let { if (it == 0.0) "" else it.toString() }
                        heightInput = (doc.getDouble(HEIGHT_FIELD) ?: 0.0).let { if (it == 0.0) "" else it.toString() }
                        selectedGoalKey = doc.getString(GOAL_FIELD) ?: ""
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
                        Text(stringResource(R.string.ques_loading), color = MaterialTheme.colorScheme.outline)
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
                    Text(stringResource(R.string.ques_current_plan), fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.ques_saved_desc),
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
                            Text(stringResource(R.string.ques_diet_label), fontSize = 18.sp,
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
                            Text(stringResource(R.string.ques_exercise_label), fontSize = 18.sp,
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
                        Text(stringResource(R.string.ques_update_btn), fontSize = 16.sp, color = Color.White)
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
                    Icon(
                        imageVector = IconList.Plan,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = IconList.AchievementGold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(if (hasExistingPlan) R.string.ques_update_title else R.string.ques_profile_title),
                        fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(stringResource(R.string.ques_tell_us), fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline)

                    Spacer(Modifier.height(32.dp))

                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { new -> if (new.all { it.isDigit() }) ageInput = new },
                        label = { Text(stringResource(R.string.ques_age)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { new -> 
                            if (new.matches(Regex("^\\d{0,3}([.,]\\d{0,2})?$"))) {
                                weightInput = new.replace(',', '.') 
                            }
                        },
                        label = { Text(stringResource(R.string.ques_weight)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { new -> 
                            if (new.matches(Regex("^\\d{0,3}([.,]\\d{0,2})?$"))) {
                                heightInput = new.replace(',', '.') 
                            }
                        },
                        label = { Text(stringResource(R.string.ques_height)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.ques_goal_section), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    GOAL_OPTIONS.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (key, resId) ->
                                FilterChip(
                                    selected = selectedGoalKey == key,
                                    onClick = { selectedGoalKey = key },
                                    label = { Text(stringResource(resId)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.ques_activity_section), fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    ACTIVITY_OPTIONS.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (key, resId) ->
                                FilterChip(
                                    selected = selectedActivityKey == key,
                                    onClick = { selectedActivityKey = key },
                                    label = { Text(stringResource(resId)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(24.dp))

                    val parsedAge = ageInput.toIntOrNull()
                    val parsedWeight = weightInput.toDoubleOrNull()
                    val parsedHeight = heightInput.toDoubleOrNull()
                    val isValid = parsedAge != null && parsedAge in 1..120 &&
                            parsedWeight != null && parsedWeight in 20.0..300.0 &&
                            parsedHeight != null && parsedHeight in 50.0..250.0 &&
                            selectedGoalKey.isNotEmpty() && selectedActivityKey.isNotEmpty()

                    // Obtenemos los textos para pasar al callback (usamos los recursos)
                    val goalLabel = if (selectedGoalKey.isNotEmpty()) 
                        stringResource(GOAL_OPTIONS.first { it.first == selectedGoalKey }.second) else ""
                    val activityLabel = if (selectedActivityKey.isNotEmpty()) 
                        stringResource(ACTIVITY_OPTIONS.first { it.first == selectedActivityKey }.second) else ""

                    Button(
                        onClick = {
                            onSubmit(parsedAge!!, parsedWeight!!, parsedHeight!!, goalLabel, activityLabel)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isValid
                    ) {
                        Text(
                            text = stringResource(if (hasExistingPlan) R.string.home_update_plan else R.string.home_generate_plan),
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
                            Text(stringResource(R.string.ques_see_plan), fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
