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

            var newCurrent = 1
            if (lastDate.isNotEmpty()) {
                if (lastDate == today) {
                    newCurrent = current
                } else if (DateUtils.isYesterday(lastDate)) {
                    newCurrent = current + 1
                }
            }
            
            val newLongest = if (newCurrent > longest) newCurrent else longest

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
                
                var effectiveCurrent = 0
                if (last.isNotEmpty()) {
                    if (DateUtils.isToday(last) || DateUtils.isYesterday(last)) {
                        effectiveCurrent = current
                    }
                }

                onResult(StreakInfo(effectiveCurrent, longest, last))
            }
    }
}
