package com.vitaai.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// Firestore constants
private const val COLLECTION_USERS = "users"
private const val COLLECTION_WATER = "water"
private const val FIELD_ML = "ml"
private const val FIELD_DATE = "date"

object WaterManager {

    /**
     * Adds water intake for the current user and returns the new total.
     */
    fun addWater(amountMl: Int, onResult: (totalToday: Int) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_WATER)
            .document(DateUtils.todayKey())

        ref.get().addOnSuccessListener { doc ->
            val current = (doc.getLong(FIELD_ML) ?: 0).toInt()
            val newTotal = (current + amountMl).coerceAtLeast(0)

            ref.set(
                mapOf(FIELD_ML to newTotal, FIELD_DATE to DateUtils.todayKey()),
                SetOptions.merge()
            ).addOnSuccessListener {
                onResult(newTotal)
            }
        }
    }

    /**
     * Resets the current day intake to zero.
     */
    fun resetToday(onDone: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_WATER)
            .document(DateUtils.todayKey())
            .set(mapOf(FIELD_ML to 0, FIELD_DATE to DateUtils.todayKey()))
            .addOnSuccessListener {
                onDone()
            }
    }

    /**
     * Loads the total water intake for the current day.
     */
    fun loadToday(onResult: (Int) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_WATER)
            .document(DateUtils.todayKey())
            .get()
            .addOnSuccessListener { doc ->
                val total = (doc.getLong(FIELD_ML) ?: 0).toInt()
                onResult(total)
            }
    }
}