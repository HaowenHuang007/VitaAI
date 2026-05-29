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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.MoodManager
import com.vitaai.app.utils.callOpenAIWithHistory
import kotlinx.coroutines.launch

private object MoodConstants {
    const val USERS_COLLECTION = "users"
    const val MOODS_COLLECTION = "moods"
    const val FIELD_GOAL = "goal"
    const val FIELD_WEIGHT = "weight"
    
    const val FIELD_MOOD_KEY = "mood"
    const val FIELD_EMOJI = "emoji"
    const val FIELD_DATE = "date"
    const val FIELD_RECOMMENDATION = "recommendation"

    const val LANG_EN = "en"
    const val LANG_ZH = "zh"
    const val LANG_FR = "fr"
    const val LANG_PT = "pt"

    const val NAME_ENGLISH = "English"
    const val NAME_CHINESE = "Chinese"
    const val NAME_FRENCH = "French"
    const val NAME_PORTUGUESE = "Portuguese"
    const val NAME_SPANISH = "Spanish"

    const val T_HOW_ARE_YOU = "how_are_you"
    const val T_MOOD_AFFECTS = "mood_affects"
    const val T_GET_RECOMMENDATION = "get_recommendation"
    const val T_DAILY_TIP = "daily_tip"
    const val T_UNDERSTOOD = "understood"
    const val T_CHAT_ERROR = "chat_error"

    const val DATE_FORMAT = "yyyy-MM-dd"
    const val PROFILE_TEMPLATE = "Goal: %s, Weight: %.1fkg"
}

data class Mood(val emoji: String, val label: String, val description: String)

@Composable
fun MoodScreen(modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var recommendation by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf("") }

    val langName = when(LanguageManager.currentLanguage.value) {
        MoodConstants.LANG_EN -> MoodConstants.NAME_ENGLISH
        MoodConstants.LANG_ZH -> MoodConstants.NAME_CHINESE
        MoodConstants.LANG_FR -> MoodConstants.NAME_FRENCH
        MoodConstants.LANG_PT -> MoodConstants.NAME_PORTUGUESE
        else -> MoodConstants.NAME_SPANISH
    }

    val moods = when(LanguageManager.currentLanguage.value) {
        MoodConstants.LANG_EN -> listOf(
            Mood("😄", "Great", "I feel energetic and motivated"),
            // TODO: This string is primarily an emoji
            Mood("😊", "Good", "I feel calm and positive"),
            // TODO: This string is primarily an emoji
            Mood("😐", "Normal", "Neither good nor bad"),
            // TODO: This string is primarily an emoji
            Mood("😔", "Sad", "I feel down"),
            // TODO: This string is primarily an emoji
            Mood("😰", "Stressed", "I feel pressure and anxiety"),
            // TODO: This string is primarily an emoji
            Mood("😴", "Tired", "I feel exhausted"),
            // TODO: This string is primarily an emoji
            Mood("😠", "Irritable", "I'm in a bad mood"),
            // TODO: This string is primarily an emoji
            Mood("🤒", "Unwell", "I feel sick or unwell")
        )
        MoodConstants.LANG_ZH -> listOf(
            // TODO: This string is primarily an emoji
            Mood("😄", "很好", "精力充沛，充满动力"),
            // TODO: This string is primarily an emoji
            Mood("😊", "好", "平静积极"),
            // TODO: This string is primarily an emoji
            Mood("😐", "一般", "不好不坏，普通的一天"),
            // TODO: This string is primarily an emoji
            Mood("😔", "悲伤", "情绪低落"),
            // TODO: This string is primarily an emoji
            Mood("😰", "压力", "压力很大，焦虑"),
            // TODO: This string is primarily an emoji
            Mood("😴", "疲惫", "精疲力竭"),
            // TODO: This string is primarily an emoji
            Mood("😠", "烦躁", "心情不好"),
            // TODO: This string is primarily an emoji
            Mood("🤒", "不适", "感觉生病或不舒服")
        )
        MoodConstants.LANG_FR -> listOf(
            // TODO: This string is primarily an emoji
            Mood("😄", "Super", "Je me sens énergique et motivé"),
            // TODO: This string is primarily an emoji
            Mood("😊", "Bien", "Je me sens calme et positif"),
            // TODO: This string is primarily an emoji
            Mood("😐", "Normal", "Ni bien ni mal"),
            // TODO: This string is primarily an emoji
            Mood("😔", "Triste", "Je me sens déprimé"),
            // TODO: This string is primarily an emoji
            Mood("😰", "Stressé", "Je ressens beaucoup de pression"),
            // TODO: This string is primarily an emoji
            Mood("😴", "Fatigué", "Je me sens épuisé"),
            // TODO: This string is primarily an emoji
            Mood("😠", "Irritable", "Je suis de mauvaise humeur"),
            // TODO: This string is primarily an emoji
            Mood("🤒", "Malade", "Je me sens malade")
        )
        MoodConstants.LANG_PT -> listOf(
            // TODO: This string is primarily an emoji
            Mood("😄", "Ótimo", "Me sinto energético e motivado"),
            // TODO: This string is primarily an emoji
            Mood("😊", "Bem", "Me sinto calmo e positivo"),
            // TODO: This string is primarily an emoji
            Mood("😐", "Normal", "Nem bem nem mal"),
            // TODO: This string is primarily an emoji
            Mood("😔", "Triste", "Me sinto para baixo"),
            // TODO: This string is primarily an emoji
            Mood("😰", "Estressado", "Sinto muita pressão e ansiedade"),
            // TODO: This string is primarily an emoji
            Mood("😴", "Cansado", "Me sinto exausto"),
            // TODO: This string is primarily an emoji
            Mood("😠", "Irritável", "Estou de mau humor"),
            // TODO: This string is primarily an emoji
            Mood("🤒", "Mal físico", "Me sinto doente")
        )
        else -> listOf(
            // TODO: This string is primarily an emoji
            Mood("😄", "Genial", "Me siento con energía y motivado"),
            // TODO: This string is primarily an emoji
            Mood("😊", "Bien", "Me siento tranquilo y positivo"),
            // TODO: This string is primarily an emoji
            Mood("😐", "Normal", "Ni bien ni mal, día normal"),
            // TODO: This string is primarily an emoji
            Mood("😔", "Triste", "Me siento bajo de ánimo"),
            // TODO: This string is primarily an emoji
            Mood("😰", "Estresado", "Tengo mucha presión y ansiedad"),
            // TODO: This string is primarily an emoji
            Mood("😴", "Cansado", "Me siento agotado y sin energía"),
            // TODO: This string is primarily an emoji
            Mood("😠", "Irritable", "Me siento de mal humor"),
            // TODO: This string is primarily an emoji
            Mood("🤒", "Mal físico", "Me siento enfermo o con malestar")
        )
    }

    LaunchedEffect(Unit) {
        db.collection(MoodConstants.USERS_COLLECTION).document(uid).get()
            .addOnSuccessListener { doc ->
                val goal = doc.getString(MoodConstants.FIELD_GOAL) ?: ""
                val weight = doc.getDouble(MoodConstants.FIELD_WEIGHT) ?: 0.0
                userProfile = MoodConstants.PROFILE_TEMPLATE.format(goal, weight)
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
        // TODO: This string is primarily an emoji
        Text("😊", fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text(LanguageManager.t(MoodConstants.T_HOW_ARE_YOU), fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center)
        Text(LanguageManager.t(MoodConstants.T_MOOD_AFFECTS), fontSize = 14.sp,
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
                        // TODO: This string is primarily an emoji
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

                                val today = java.text.SimpleDateFormat(MoodConstants.DATE_FORMAT,
                                    java.util.Locale.getDefault()).format(java.util.Date())
                                db.collection(MoodConstants.USERS_COLLECTION).document(uid)
                                    .collection(MoodConstants.MOODS_COLLECTION).document(today)
                                    .set(mapOf(
                                        MoodConstants.FIELD_MOOD_KEY to mood.label,
                                        MoodConstants.FIELD_EMOJI to mood.emoji,
                                        MoodConstants.FIELD_DATE to today,
                                        MoodConstants.FIELD_RECOMMENDATION to recommendation
                                    )).addOnSuccessListener {
                                        // Marca localmente que ya se registró hoy
                                        MoodManager.saveMoodLocally(context, mood.label, mood.emoji, recommendation)
                                    }
                            } catch (e: Exception) {
                                recommendation = LanguageManager.t(MoodConstants.T_CHAT_ERROR)
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(LanguageManager.t(MoodConstants.T_GET_RECOMMENDATION), fontSize = 15.sp)
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
                        // Emoji removed: 🧠
                        Text(LanguageManager.t(MoodConstants.T_DAILY_TIP), fontSize = 16.sp,
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
                    Text(LanguageManager.t(MoodConstants.T_UNDERSTOOD), fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
