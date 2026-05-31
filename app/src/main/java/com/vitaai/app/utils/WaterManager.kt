package com.vitaai.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

object WaterManager {

    fun addWater(amountMl: Int, onResult: (totalToday: Int) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("water").document(DateUtils.todayKey())
        ref.get().addOnSuccessListener { doc ->
            val current = (doc.getLong("ml") ?: 0).toInt()
            val newTotal = (current + amountMl).coerceAtLeast(0)
            ref.set(
                mapOf("ml" to newTotal, "date" to DateUtils.todayKey()),
                SetOptions.merge()
            ).addOnSuccessListener { onResult(newTotal) }
             .addOnFailureListener { onResult(-1) }
        }
    }

    fun resetToday(onDone: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("water").document(DateUtils.todayKey())
            .set(mapOf("ml" to 0, "date" to DateUtils.todayKey()))
            .addOnSuccessListener { onDone() }
    }

    fun loadToday(onResult: (Int) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("water").document(DateUtils.todayKey())
            .get()
            .addOnSuccessListener { doc ->
                onResult((doc.getLong("ml") ?: 0).toInt())
            }
    }

    fun loadHistory(limit: Int = 7, onResult: (List<Pair<String, Int>>) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("water")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { snap ->
                onResult(snap.documents.mapNotNull { doc ->
                    val date = doc.getString("date") ?: return@mapNotNull null
                    date to (doc.getLong("ml") ?: 0).toInt()
                })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}
