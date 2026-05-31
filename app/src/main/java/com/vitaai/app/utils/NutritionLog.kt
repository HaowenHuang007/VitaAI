package com.vitaai.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object NutritionLog {

    data class DailyTotals(
        val calories: Int = 0,
        val proteinG: Int = 0,
        val carbsG: Int = 0,
        val fatG: Int = 0
    )

    fun addMeal(
        calories: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
        onResult: (DailyTotals) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("nutrition").document(DateUtils.todayKey())
        ref.get().addOnSuccessListener { doc ->
            val totals = DailyTotals(
                calories = ((doc.getLong("calories") ?: 0) + calories).toInt(),
                proteinG = ((doc.getLong("proteinG") ?: 0) + proteinG).toInt(),
                carbsG = ((doc.getLong("carbsG") ?: 0) + carbsG).toInt(),
                fatG = ((doc.getLong("fatG") ?: 0) + fatG).toInt()
            )
            ref.set(
                mapOf(
                    "calories" to totals.calories,
                    "proteinG" to totals.proteinG,
                    "carbsG" to totals.carbsG,
                    "fatG" to totals.fatG,
                    "date" to DateUtils.todayKey()
                ),
                SetOptions.merge()
            ).addOnSuccessListener { onResult(totals) }
                .addOnFailureListener { onResult(totals) }
        }
    }

    fun loadToday(onResult: (DailyTotals) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("nutrition").document(DateUtils.todayKey())
            .get()
            .addOnSuccessListener { doc ->
                onResult(
                    DailyTotals(
                        calories = (doc.getLong("calories") ?: 0).toInt(),
                        proteinG = (doc.getLong("proteinG") ?: 0).toInt(),
                        carbsG = (doc.getLong("carbsG") ?: 0).toInt(),
                        fatG = (doc.getLong("fatG") ?: 0).toInt()
                    )
                )
            }
    }
}
