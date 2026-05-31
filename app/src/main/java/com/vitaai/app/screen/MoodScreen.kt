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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.utils.MoodManager
import com.vitaai.app.utils.callOpenAIWithHistory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val USERS_COLLECTION = "users"
private const val MOODS_COLLECTION = "moods"
private const val FIELD_GOAL = "goal"
private const val FIELD_WEIGHT = "weight"

private const val FIELD_MOOD_KEY = "mood"
private const val FIELD_MOOD_ID = "moodId"
private const val FIELD_DATE = "date"
private const val FIELD_RECOMMENDATION = "recommendation"

private const val DATE_FORMAT = "yyyy-MM-dd"
private const val PROFILE_TEMPLATE = "Goal: %s, Weight: %.1fkg"

data class MoodItem(
    val id: String,
    val labelRes: Int,
    val descRes: Int
)

@Composable
fun MoodScreen(modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var selectedMood by remember { mutableStateOf<MoodItem?>(null) }
    var recommendation by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf("") }

    val locale = Locale.getDefault()
    val langName = locale.displayLanguage

    val moods = listOf(
        MoodItem("great", R.string.mood_great_label, R.string.mood_great_desc),
        MoodItem("good", R.string.mood_good_label, R.string.mood_good_desc),
        MoodItem("normal", R.string.mood_normal_label, R.string.mood_normal_desc),
        MoodItem("sad", R.string.mood_sad_label, R.string.mood_sad_desc),
        MoodItem("stressed", R.string.mood_stressed_label, R.string.mood_stressed_desc),
        MoodItem("tired", R.string.mood_tired_label, R.string.mood_tired_desc),
        MoodItem("irritable", R.string.mood_irritable_label, R.string.mood_irritable_desc),
        MoodItem("unwell", R.string.mood_unwell_label, R.string.mood_unwell_desc)
    )

    LaunchedEffect(Unit) {
        db.collection(USERS_COLLECTION).document(uid).get()
            .addOnSuccessListener { doc ->
                val goal = doc.getString(FIELD_GOAL) ?: ""
                val weight = doc.getDouble(FIELD_WEIGHT) ?: 0.0
                userProfile = PROFILE_TEMPLATE.format(goal, weight)
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
        Icon(
            imageVector = IconList.Mood,
            contentDescription = null,
            tint = IconList.MoodYellow,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.mood_how_are_you),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.mood_affects_nutrition),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        moods.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        Icon(
                            imageVector = IconList.getIconForMood(mood.id),
                            contentDescription = null,
                            tint = IconList.getColorForMood(mood.id),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(mood.labelRes),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        selectedMood?.let { mood ->
            val moodLabel = stringResource(mood.labelRes)
            val moodDesc = stringResource(mood.descRes)

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
                                    TODAY'S EMOTIONAL STATE: $moodLabel - $moodDesc
                                    Respond with:
                                    1. Why this state affects nutrition (1 sentence)
                                    2. 3 specific foods recommended TODAY and why
                                    3. 1 habit tip to improve the state
                                    Be empathetic, specific and motivating. Max 150 words.
                                """.trimIndent()
                                recommendation = callOpenAIWithHistory(prompt, emptyList())

                                val today = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
                                db.collection(USERS_COLLECTION).document(uid)
                                    .collection(MOODS_COLLECTION).document(today)
                                    .set(mapOf(
                                        FIELD_MOOD_KEY to moodLabel,
                                        FIELD_MOOD_ID to mood.id,
                                        FIELD_DATE to today,
                                        FIELD_RECOMMENDATION to recommendation
                                    )).addOnSuccessListener {
                                        MoodManager.saveMoodLocally(context, moodLabel, mood.id, recommendation)
                                    }
                            } catch (e: Exception) {
                                recommendation = context.getString(R.string.chat_error)
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.mood_get_recommendation), fontSize = 15.sp)
                }
            }

            if (isLoading) {
                Spacer(Modifier.height(32.dp))
                CircularProgressIndicator()
            }

            if (recommendation.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.mood_daily_tip),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.mood_understood), fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
