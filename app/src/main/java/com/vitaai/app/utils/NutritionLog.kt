package com.vitaai.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

private const val COLLECTION_USERS = "users"
private const val COLLECTION_NUTRITION = "nutrition"
private const val FIELD_CALORIES = "calories"
private const val FIELD_PROTEIN = "proteinG"
private const val FIELD_CARBS = "carbsG"
private const val FIELD_FAT = "fatG"
private const val FIELD_DATE = "date"

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
        val ref = FirebaseFirestore.getInstance().collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_NUTRITION).document(DateUtils.todayKey())
            
        ref.get().addOnSuccessListener { doc ->
            val oldCalories = (doc.getLong(FIELD_CALORIES) ?: 0).toInt()
            val oldProtein = (doc.getLong(FIELD_PROTEIN) ?: 0).toInt()
            val oldCarbs = (doc.getLong(FIELD_CARBS) ?: 0).toInt()
            val oldFat = (doc.getLong(FIELD_FAT) ?: 0).toInt()

            val totals = DailyTotals(
                calories = oldCalories + calories,
                proteinG = oldProtein + proteinG,
                carbsG = oldCarbs + carbsG,
                fatG = oldFat + fatG
            )
            
            ref.set(
                mapOf(
                    FIELD_CALORIES to totals.calories,
                    FIELD_PROTEIN to totals.proteinG,
                    FIELD_CARBS to totals.carbsG,
                    FIELD_FAT to totals.fatG,
                    FIELD_DATE to DateUtils.todayKey()
                ),
                SetOptions.merge()
            ).addOnSuccessListener { onResult(totals) }
        }
    }

    fun loadToday(onResult: (DailyTotals) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection(COLLECTION_USERS).document(uid)
            .collection(COLLECTION_NUTRITION).document(DateUtils.todayKey())
            .get()
            .addOnSuccessListener { doc ->
                val totals = DailyTotals(
                    calories = (doc.getLong(FIELD_CALORIES) ?: 0).toInt(),
                    proteinG = (doc.getLong(FIELD_PROTEIN) ?: 0).toInt(),
                    carbsG = (doc.getLong(FIELD_CARBS) ?: 0).toInt(),
                    fatG = (doc.getLong(FIELD_FAT) ?: 0).toInt()
                )
                onResult(totals)
            }
    }
}
