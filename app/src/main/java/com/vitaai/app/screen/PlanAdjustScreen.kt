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
import com.vitaai.app.utils.callOpenAI
import kotlinx.coroutines.launch

@Composable
fun PlanAdjustScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysis by remember { mutableStateOf("") }
    var newDietPlan by remember { mutableStateOf("") }
    var newExercisePlan by remember { mutableStateOf("") }
    var savedMsg by remember { mutableStateOf("") }
    var userGoal by remember { mutableStateOf("") }
    var progressData by remember { mutableStateOf<List<ProgressEntry>>(emptyList()) }
    var initialWeight by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        // Cargar perfil
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userGoal = doc.getString("goal") ?: ""
                initialWeight = doc.getDouble("weight") ?: 0.0
            }

        // Cargar últimas 2 semanas de progreso
        db.collection("users").document(uid)
            .collection("progress")
            .orderBy("date")
            .limit(14)
            .get()
            .addOnSuccessListener { snap ->
                progressData = snap.documents.mapNotNull {
                    it.toObject(ProgressEntry::class.java)
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("📈 Ajuste Inteligente de Plan", fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("La IA analiza tu progreso real y ajusta tu plan",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)

        Spacer(Modifier.height(24.dp))

        // Resumen de progreso
        if (progressData.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Tu progreso reciente", fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))

                    val firstWeight = progressData.first().weight
                    val lastWeight = progressData.last().weight
                    val weightChange = lastWeight - firstWeight
                    val avgCalories = progressData.map { it.calories }.average()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard("Peso inicial", "${firstWeight}kg")
                        StatCard("Peso actual", "${lastWeight}kg")
                        StatCard(
                            "Cambio",
                            "${if (weightChange >= 0) "+" else ""}${"%.1f".format(weightChange)}kg"
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Calorías promedio: ${avgCalories.toInt()} kcal/día",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(4.dp))
                    Text("Objetivo: $userGoal", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    isAnalyzing = true
                    analysis = ""
                    newDietPlan = ""
                    newExercisePlan = ""
                    scope.launch {
                        try {
                            val firstWeight = progressData.first().weight
                            val lastWeight = progressData.last().weight
                            val weightChange = lastWeight - firstWeight
                            val avgCalories = progressData.map { it.calories }.average()
                            val days = progressData.size

                            val prompt = """
                                Eres un nutricionista y entrenador personal experto.
                                
                                DATOS DEL USUARIO:
                                - Objetivo: $userGoal
                                - Peso inicial (hace $days días): ${firstWeight}kg
                                - Peso actual: ${lastWeight}kg
                                - Cambio de peso: ${weightChange}kg
                                - Calorías promedio diarias: ${avgCalories.toInt()} kcal
                                
                                ANÁLISIS REQUERIDO:
                                Basándote en estos datos reales, responde SOLO en este JSON exacto:
                                {
                                  "analysis": "análisis breve del progreso en 2 frases",
                                  "diet": "nuevo plan de dieta ajustado y específico",
                                  "exercise": "nuevo plan de ejercicio ajustado y específico"
                                }
                                
                                Si el progreso es bueno, mantén la dirección pero optimiza.
                                Si el progreso es lento o negativo, cambia significativamente el enfoque.
                            """.trimIndent()

                            val result = callOpenAI(prompt)
                            val json = org.json.JSONObject(result)
                            analysis = json.getString("analysis")
                            newDietPlan = json.getString("diet")
                            newExercisePlan = json.getString("exercise")

                        } catch (e: Exception) {
                            analysis = "Error al analizar: ${e.message}"
                        }
                        isAnalyzing = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Analizando tu progreso...")
                } else {
                    Text("🤖 Analizar y ajustar mi plan", fontSize = 16.sp)
                }
            }

            if (analysis.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))

                // Análisis
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🧠 Análisis de tu progreso", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(analysis, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Nuevo plan dieta
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🥗 Nuevo Plan de Dieta", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(newDietPlan, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Nuevo plan ejercicio
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💪 Nuevo Plan de Ejercicio", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(newExercisePlan, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (savedMsg.isNotEmpty()) {
                    Text(savedMsg, color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        db.collection("users").document(uid)
                            .update(mapOf(
                                "dietPlan" to newDietPlan,
                                "exercisePlan" to newExercisePlan
                            ))
                            .addOnSuccessListener {
                                savedMsg = "✅ Plan actualizado en tu perfil"
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💾 Guardar nuevo plan", fontSize = 16.sp)
                }
            }

        } else {
            Spacer(Modifier.height(48.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Aún no tienes datos de progreso",
                        fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("Registra al menos unos días en 'Mi progreso'\npara que la IA pueda ajustar tu plan",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
    }
}