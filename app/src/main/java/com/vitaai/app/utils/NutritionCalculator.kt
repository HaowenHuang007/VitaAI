package com.vitaai.app.utils

import kotlin.math.roundToInt

object NutritionCalculator {

    data class DailyTargets(
        val calories: Int,
        val proteinG: Int,
        val carbsG: Int,
        val fatG: Int,
        val waterMl: Int,
        val bmr: Int,
        val tdee: Int
    )

    // Mifflin-St Jeor BMR (asumimos sexo no especificado, usamos promedio entre hombre y mujer)
    fun bmr(weightKg: Double, heightCm: Double, ageYears: Int): Int {
        val male = 10 * weightKg + 6.25 * heightCm - 5 * ageYears + 5
        val female = 10 * weightKg + 6.25 * heightCm - 5 * ageYears - 161
        return ((male + female) / 2).roundToInt()
    }

    private fun activityMultiplier(activityKey: String): Double = when (activityKey) {
        "sedentary" -> 1.2
        "light" -> 1.375
        "moderate" -> 1.55
        "active" -> 1.725
        "very_active" -> 1.9
        else -> 1.4
    }

    private fun goalCalorieAdjustment(goalKey: String): Int = when (goalKey) {
        "lose_weight" -> -400
        "gain_muscle" -> +300
        "maintain_weight" -> 0
        "improve_health" -> -100
        else -> 0
    }

    fun computeTargets(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        activityKey: String,
        goalKey: String
    ): DailyTargets {
        val bmr = bmr(weightKg, heightCm, ageYears)
        val tdee = (bmr * activityMultiplier(activityKey)).roundToInt()
        val targetCalories = (tdee + goalCalorieAdjustment(goalKey)).coerceAtLeast(1200)

        val proteinPct = if (goalKey == "gain_muscle") 0.35 else 0.30
        val fatPct = 0.30
        val carbsPct = 1.0 - proteinPct - fatPct

        val proteinG = (targetCalories * proteinPct / 4).roundToInt()
        val carbsG = (targetCalories * carbsPct / 4).roundToInt()
        val fatG = (targetCalories * fatPct / 9).roundToInt()
        val waterMl = (weightKg * 35).roundToInt()

        return DailyTargets(
            calories = targetCalories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            waterMl = waterMl,
            bmr = bmr,
            tdee = tdee
        )
    }

    fun bmi(weightKg: Double, heightCm: Double): Double {
        val m = heightCm / 100
        return weightKg / (m * m)
    }

    fun bmiCategoryKey(bmi: Double): String = when {
        bmi < 18.5 -> "bmi_underweight"
        bmi < 25.0 -> "bmi_normal"
        bmi < 30.0 -> "bmi_overweight"
        else -> "bmi_obese"
    }
}
