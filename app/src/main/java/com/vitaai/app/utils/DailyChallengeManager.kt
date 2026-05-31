package com.vitaai.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.random.Random

object DailyChallengeManager {

    data class Challenge(
        val id: String,
        val emoji: String,
        val titleKey: String,
        val descKey: String,
        val xpReward: Int
    )

    val ALL: List<Challenge> = listOf(
        Challenge("ch_water_2l", "💧", "ch_water_2l_t", "ch_water_2l_d", 20),
        Challenge("ch_scan_food", "📸", "ch_scan_food_t", "ch_scan_food_d", 15),
        Challenge("ch_chat", "💬", "ch_chat_t", "ch_chat_d", 10),
        Challenge("ch_log_weight", "⚖️", "ch_log_weight_t", "ch_log_weight_d", 15),
        Challenge("ch_exercise", "🏋️", "ch_exercise_t", "ch_exercise_d", 25),
        Challenge("ch_mood", "😊", "ch_mood_t", "ch_mood_d", 10),
        Challenge("ch_3_meals", "🍽️", "ch_3_meals_t", "ch_3_meals_d", 30),
        Challenge("ch_no_sugar", "🚫", "ch_no_sugar_t", "ch_no_sugar_d", 30),
        Challenge("ch_walk_5k", "🚶", "ch_walk_5k_t", "ch_walk_5k_d", 25),
        Challenge("ch_protein", "💪", "ch_protein_t", "ch_protein_d", 20)
    )

    data class TodayChallenge(val challenge: Challenge, val completed: Boolean)

    fun loadToday(onResult: (TodayChallenge) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val today = DateUtils.todayKey()
        val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("challenges").document(today)
        ref.get().addOnSuccessListener { doc ->
            val challenge: Challenge
            val completed: Boolean
            if (doc.exists()) {
                val id = doc.getString("id") ?: ALL[0].id
                challenge = ALL.find { it.id == id } ?: ALL[0]
                completed = doc.getBoolean("completed") ?: false
            } else {
                // deterministic per-day selection
                val seed = today.hashCode().toLong()
                challenge = ALL[Random(seed).nextInt(ALL.size)]
                completed = false
                ref.set(mapOf(
                    "id" to challenge.id,
                    "completed" to false,
                    "date" to today
                ))
            }
            onResult(TodayChallenge(challenge, completed))
        }
    }

    fun markCompleted(onDone: (xp: Int) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val today = DateUtils.todayKey()
        val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("challenges").document(today)
        ref.get().addOnSuccessListener { doc ->
            if (doc.getBoolean("completed") == true) { onDone(0); return@addOnSuccessListener }
            val id = doc.getString("id") ?: return@addOnSuccessListener
            val challenge = ALL.find { it.id == id } ?: return@addOnSuccessListener
            ref.set(mapOf("completed" to true), SetOptions.merge())
            XPManager.addXP(challenge.xpReward, "challenge_$id")
            StreakManager.recordActivity()
            onDone(challenge.xpReward)
        }
    }
}
