package com.vitaai.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object AchievementManager {

    data class Achievement(
        val id: String,
        val emoji: String,
        val titleKey: String,
        val descKey: String,
        val xpReward: Int
    )

    val ALL: List<Achievement> = listOf(
        Achievement("first_plan", "🤖", "ach_first_plan_t", "ach_first_plan_d", 30),
        Achievement("first_food", "📸", "ach_first_food_t", "ach_first_food_d", 20),
        Achievement("first_chat", "💬", "ach_first_chat_t", "ach_first_chat_d", 10),
        Achievement("first_mood", "😊", "ach_first_mood_t", "ach_first_mood_d", 10),
        Achievement("first_water", "💧", "ach_first_water_t", "ach_first_water_d", 10),
        Achievement("first_exercise", "🏋️", "ach_first_exercise_t", "ach_first_exercise_d", 20),
        Achievement("streak_3", "🔥", "ach_streak3_t", "ach_streak3_d", 30),
        Achievement("streak_7", "🔥", "ach_streak7_t", "ach_streak7_d", 60),
        Achievement("streak_30", "🔥", "ach_streak30_t", "ach_streak30_d", 200),
        Achievement("water_goal", "💧", "ach_water_goal_t", "ach_water_goal_d", 25),
        Achievement("food_10", "🍽️", "ach_food10_t", "ach_food10_d", 50),
        Achievement("food_50", "🍽️", "ach_food50_t", "ach_food50_d", 150),
        Achievement("level_3", "⭐", "ach_level3_t", "ach_level3_d", 50),
        Achievement("level_5", "👑", "ach_level5_t", "ach_level5_d", 200),
        Achievement("goal_reached", "🎯", "ach_goal_t", "ach_goal_d", 200)
    )

    fun unlock(id: String, onUnlocked: (Achievement) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val achievement = ALL.find { it.id == id } ?: return
        val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
        ref.get().addOnSuccessListener { doc ->
            @Suppress("UNCHECKED_CAST")
            val unlocked = (doc.get("achievements") as? List<String>) ?: emptyList()
            if (id in unlocked) return@addOnSuccessListener
            ref.update("achievements", FieldValue.arrayUnion(id))
                .addOnSuccessListener {
                    XPManager.addXP(achievement.xpReward, "achievement_$id")
                    onUnlocked(achievement)
                }
        }
    }

    fun loadUnlocked(onResult: (Set<String>) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val unlocked = (doc.get("achievements") as? List<String>) ?: emptyList()
                onResult(unlocked.toSet())
            }
    }
}
