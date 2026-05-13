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

    val goalKeys = listOf("lose_weight", "gain_muscle", "maintain_weight", "improve_health")
    val activityKeys = listOf("sedentary", "light", "moderate", "active", "very_active")

    LaunchedEffect(Unit) {
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val diet = doc.getString("dietPlan") ?: ""
                    val exercise = doc.getString("exercisePlan") ?: ""
                    if (diet.isNotEmpty() && exercise.isNotEmpty()) {
                        existingDietPlan = diet
                        existingExercisePlan = exercise
                        hasExistingPlan = true
                        age = (doc.getLong("age") ?: 0).let { if (it == 0L) "" else it.toString() }
                        weight = (doc.getDouble("weight") ?: 0.0).let { if (it == 0.0) "" else it.toString() }
                        height = (doc.getDouble("height") ?: 0.0).let { if (it == 0.0) "" else it.toString() }
                        selectedGoal = doc.getString("goal") ?: ""
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
                        Text(LanguageManager.t("loading_plan"), color = MaterialTheme.colorScheme.outline)
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
                    Text(LanguageManager.t("current_plan"), fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(LanguageManager.t("saved_plan"),
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
                            Text(LanguageManager.t("diet_plan"), fontSize = 18.sp,
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
                            Text(LanguageManager.t("exercise_plan"), fontSize = 18.sp,
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
                        Text(LanguageManager.t("update_my_plan"), fontSize = 16.sp, color = Color.White)
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
                    Text("📋", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (hasExistingPlan) LanguageManager.t("update_profile") else LanguageManager.t("profile"),
                        fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(LanguageManager.t("tell_us"), fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline)

                    Spacer(Modifier.height(32.dp))

                    OutlinedTextField(
                        value = age,
                        onValueChange = { new -> if (new.all { it.isDigit() }) age = new },
                        label = { Text(LanguageManager.t("age")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { new -> if (new.matches(Regex("^\\d{0,3}([.,]\\d{0,2})?$"))) weight = new.replace(',', '.') },
                        label = { Text(LanguageManager.t("weight")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = height,
                        onValueChange = { new -> if (new.matches(Regex("^\\d{0,3}([.,]\\d{0,2})?$"))) height = new.replace(',', '.') },
                        label = { Text(LanguageManager.t("height")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Spacer(Modifier.height(24.dp))
                    Text(LanguageManager.t("goal"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    goalKeys.chunked(2).forEach { row ->
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
                    Text(LanguageManager.t("activity_level"), fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    activityKeys.chunked(2).forEach { row ->
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
                            if (hasExistingPlan) LanguageManager.t("update_plan")
                            else LanguageManager.t("generate_plan"),
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
                            Text(LanguageManager.t("see_current_plan"), fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
