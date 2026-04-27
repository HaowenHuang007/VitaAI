package com.vitaai.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

object XPManager {

    data class LevelInfo(
        val level: Int,
        val title: String,
        val emoji: String,
        val currentXP: Int,
        val xpForNext: Int,
        val progress: Float
    )

    fun getLevelInfo(xp: Int): LevelInfo {
        return when {
            xp >= 1000 -> LevelInfo(5, "Maestro", "👑", xp, 1000, 1f)
            xp >= 600 -> LevelInfo(4, "Experto", "⭐", xp, 1000, (xp - 600) / 400f)
            xp >= 300 -> LevelInfo(3, "Atleta", "💪", xp, 600, (xp - 300) / 300f)
            xp >= 100 -> LevelInfo(2, "Aprendiz", "🌿", xp, 300, (xp - 100) / 200f)
            else -> LevelInfo(1, "Principiante", "🌱", xp, 100, xp / 100f)
        }
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Añade XP con límite diario por acción
    fun addXPWithLimit(
        amount: Int,
        actionKey: String,
        dailyLimit: Int = 1,
        onAdded: (Int) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)
        val today = today()
        val limitKey = "xpLimit_${actionKey}_$today"

        userRef.get().addOnSuccessListener { doc ->
            val currentCount = (doc.getLong(limitKey) ?: 0).toInt()
            if (currentCount >= dailyLimit) {
                onAdded(0) // Ya alcanzó el límite
                return@addOnSuccessListener
            }
            val currentXP = (doc.getLong("xp") ?: 0).toInt()
            db.runTransaction { transaction ->
                transaction.set(
                    userRef,
                    mapOf(
                        "xp" to (currentXP + amount),
                        limitKey to (currentCount + 1)
                    ),
                    SetOptions.merge()
                )
            }.addOnSuccessListener { onAdded(amount) }
        }
    }

    // Versión simple sin callback
    fun addXP(amount: Int, reason: String = "") {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)
        db.runTransaction { transaction ->
            val doc = transaction.get(userRef)
            val currentXP = (doc.getLong("xp") ?: 0).toInt()
            transaction.set(
                userRef,
                mapOf("xp" to (currentXP + amount), "lastXPReason" to reason),
                SetOptions.merge()
            )
        }
    }

    const val XP_DAILY_LOG = 10
    const val XP_GENERATE_PLAN = 20
    const val XP_CHAT_MESSAGE = 5
    const val XP_FOOD_SCAN = 15
    const val XP_MOOD_CHECK = 5

    // Keys para límites diarios
    const val KEY_PLAN = "plan"
    const val KEY_PROGRESS = "progress"
    const val KEY_CHAT = "chat"
    const val KEY_FOOD = "food"
    const val KEY_MOOD = "mood"
}