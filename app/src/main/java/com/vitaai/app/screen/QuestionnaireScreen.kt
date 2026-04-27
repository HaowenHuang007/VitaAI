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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.ui.theme.NavyBlue

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

    val goals = listOf("Perder peso", "Ganar músculo", "Mantener peso", "Mejorar salud")
    val activities = listOf("Sedentario", "Ligero", "Moderado", "Activo", "Muy activo")

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
            // Loading
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando tu plan...", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Mostrar plan existente
            hasExistingPlan && !showQuestionnaire -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text("🤖 Tu Plan Actual", fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Tu plan personalizado guardado",
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
                            Text("🥗 Plan de Dieta", fontSize = 18.sp,
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
                            Text("💪 Plan de Ejercicio", fontSize = 18.sp,
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
                        Text("🔄 Actualizar mi plan", fontSize = 16.sp, color = Color.White)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }

            // Mostrar cuestionario
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
                        if (hasExistingPlan) "Actualizar tu perfil" else "Tu perfil",
                        fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Cuéntanos sobre ti", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline)

                    Spacer(Modifier.height(32.dp))

                    OutlinedTextField(
                        value = age, onValueChange = { age = it },
                        label = { Text("Edad") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = weight, onValueChange = { weight = it },
                        label = { Text("Peso (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = height, onValueChange = { height = it },
                        label = { Text("Altura (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))
                    Text("🎯 Objetivo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    goals.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { goal ->
                                FilterChip(
                                    selected = selectedGoal == goal,
                                    onClick = { selectedGoal = goal },
                                    label = { Text(goal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("🏃 Nivel de actividad", fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    activities.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { activity ->
                                FilterChip(
                                    selected = selectedActivity == activity,
                                    onClick = { selectedActivity = activity },
                                    label = { Text(activity) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(24.dp))

                    val isValid = age.isNotEmpty() && weight.isNotEmpty() &&
                            height.isNotEmpty() && selectedGoal.isNotEmpty() &&
                            selectedActivity.isNotEmpty()

                    Button(
                        onClick = {
                            onSubmit(age.toInt(), weight.toDouble(),
                                height.toDouble(), selectedGoal, selectedActivity)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isValid
                    ) {
                        Text(
                            if (hasExistingPlan) "Actualizar plan con IA 🔄"
                            else "Generar mi plan con IA 🤖",
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
                            Text("← Ver plan actual", fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}