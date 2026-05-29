package com.vitaai.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object StreakManager {

    data class StreakInfo(val current: Int, val longest: Int, val lastDate: String)

    fun recordActivity(onResult: (StreakInfo) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
        ref.get().addOnSuccessListener { doc ->
            val today = DateUtils.todayKey()
            val lastDate = doc.getString("streakLastDate") ?: ""
            val current = (doc.getLong("streakCurrent") ?: 0).toInt()
            val longest = (doc.getLong("streakLongest") ?: 0).toInt()

            val newCurrent = when {
                lastDate.isEmpty() -> 1
                lastDate == today -> current
                DateUtils.isYesterday(lastDate) -> current + 1
                else -> 1
            }
            val newLongest = maxOf(longest, newCurrent)

            ref.set(
                mapOf(
                    "streakCurrent" to newCurrent,
                    "streakLongest" to newLongest,
                    "streakLastDate" to today
                ),
                SetOptions.merge()
            ).addOnSuccessListener {
                onResult(StreakInfo(newCurrent, newLongest, today))
            }
        }
    }

    fun loadStreak(onResult: (StreakInfo) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val current = (doc.getLong("streakCurrent") ?: 0).toInt()
                val longest = (doc.getLong("streakLongest") ?: 0).toInt()
                val last = doc.getString("streakLastDate") ?: ""
                // si el último día no es hoy ni ayer, racha rota
                val effectiveCurrent = when {
                    last.isEmpty() -> 0
                    DateUtils.isToday(last) -> current
                    DateUtils.isYesterday(last) -> current
                    else -> 0
                }
                onResult(StreakInfo(effectiveCurrent, longest, last))
            }
    }
}
