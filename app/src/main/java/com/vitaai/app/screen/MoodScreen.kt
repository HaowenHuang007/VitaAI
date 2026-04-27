package com.vitaai.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.utils.LanguageManager
import kotlinx.coroutines.launch

data class Mood(val emoji: String, val label: String, val description: String)

@Composable
fun MoodScreen(modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var recommendation by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf("") }

    val langName = when(LanguageManager.currentLanguage.value) {
        "en" -> "English"
        "zh" -> "Chinese"
        "fr" -> "French"
        "pt" -> "Portuguese"
        else -> "Spanish"
    }

    val moods = when(LanguageManager.currentLanguage.value) {
        "en" -> listOf(
            Mood("😄", "Great", "I feel energetic and motivated"),
            Mood("😊", "Good", "I feel calm and positive"),
            Mood("😐", "Normal", "Neither good nor bad"),
            Mood("😔", "Sad", "I feel down"),
            Mood("😰", "Stressed", "I feel pressure and anxiety"),
            Mood("😴", "Tired", "I feel exhausted"),
            Mood("😠", "Irritable", "I'm in a bad mood"),
            Mood("🤒", "Unwell", "I feel sick or unwell")
        )
        "zh" -> listOf(
            Mood("😄", "很好", "精力充沛，充满动力"),
            Mood("😊", "好", "平静积极"),
            Mood("😐", "一般", "不好不坏，普通的一天"),
            Mood("😔", "悲伤", "情绪低落"),
            Mood("😰", "压力", "压力很大，焦虑"),
            Mood("😴", "疲惫", "精疲力竭"),
            Mood("😠", "烦躁", "心情不好"),
            Mood("🤒", "不适", "感觉生病或不舒服")
        )
        "fr" -> listOf(
            Mood("😄", "Super", "Je me sens énergique et motivé"),
            Mood("😊", "Bien", "Je me sens calme et positif"),
            Mood("😐", "Normal", "Ni bien ni mal"),
            Mood("😔", "Triste", "Je me sens déprimé"),
            Mood("😰", "Stressé", "Je ressens beaucoup de pression"),
            Mood("😴", "Fatigué", "Je me sens épuisé"),
            Mood("😠", "Irritable", "Je suis de mauvaise humeur"),
            Mood("🤒", "Malade", "Je me sens malade")
        )
        "pt" -> listOf(
            Mood("😄", "Ótimo", "Me sinto energético e motivado"),
            Mood("😊", "Bem", "Me sinto calmo e positivo"),
            Mood("😐", "Normal", "Nem bem nem mal"),
            Mood("😔", "Triste", "Me sinto para baixo"),
            Mood("😰", "Estressado", "Sinto muita pressão e ansiedade"),
            Mood("😴", "Cansado", "Me sinto exausto"),
            Mood("😠", "Irritável", "Estou de mau humor"),
            Mood("🤒", "Mal físico", "Me sinto doente")
        )
        else -> listOf(
            Mood("😄", "Genial", "Me siento con energía y motivado"),
            Mood("😊", "Bien", "Me siento tranquilo y positivo"),
            Mood("😐", "Normal", "Ni bien ni mal, día normal"),
            Mood("😔", "Triste", "Me siento bajo de ánimo"),
            Mood("😰", "Estresado", "Tengo mucha presión y ansiedad"),
            Mood("😴", "Cansado", "Me siento agotado y sin energía"),
            Mood("😠", "Irritable", "Me siento de mal humor"),
            Mood("🤒", "Mal físico", "Me siento enfermo o con malestar")
        )
    }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val goal = doc.getString("goal") ?: ""
                val weight = doc.getDouble("weight") ?: 0.0
                userProfile = "Goal: $goal, Weight: ${weight}kg"
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
        Text("😊", fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text(LanguageManager.t("how_are_you"), fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center)
        Text(LanguageManager.t("mood_affects"), fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center)

        Spacer(Modifier.height(32.dp))

        moods.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { mood ->
                    val isSelected = selectedMood == mood
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                if (!isLoading && recommendation.isEmpty()) selectedMood = mood
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(mood.emoji, fontSize = 32.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(mood.label, fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        selectedMood?.let { mood ->
            if (recommendation.isEmpty() && !isLoading) {
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val prompt = """
                                    You are an expert nutritionist in psychonutrition.
                                    RESPOND ONLY IN $langName.
                                    PROFILE: $userProfile
                                    TODAY'S EMOTIONAL STATE: ${mood.emoji} ${mood.label} - ${mood.description}
                                    Respond with:
                                    1. Why this state affects nutrition (1 sentence)
                                    2. 3 specific foods recommended TODAY and why
                                    3. 1 habit tip to improve the state
                                    Be empathetic, specific and motivating. Max 150 words.
                                """.trimIndent()
                                recommendation = callOpenAIWithHistory(prompt, emptyList())

                                val today = java.text.SimpleDateFormat("yyyy-MM-dd",
                                    java.util.Locale.getDefault()).format(java.util.Date())
                                db.collection("users").document(uid)
                                    .collection("moods").document(today)
                                    .set(mapOf(
                                        "mood" to mood.label,
                                        "emoji" to mood.emoji,
                                        "date" to today,
                                        "recommendation" to recommendation
                                    ))
                            } catch (e: Exception) {
                                recommendation = "Error"
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(LanguageManager.t("get_recommendation"), fontSize = 15.sp)
                }
            }

            if (isLoading) {
                Spacer(Modifier.height(32.dp))
                CircularProgressIndicator()
            }

            if (recommendation.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🧠 ${LanguageManager.t("daily_tip")}", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(recommendation, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(LanguageManager.t("understood"), fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}