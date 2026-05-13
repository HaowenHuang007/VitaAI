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
        "sedentary", "Sedentario", "Sedentary", "久坐", "Sédentaire", "Sedentário" -> 1.2
        "light", "Ligero", "Light", "轻度", "Léger", "Leve" -> 1.375
        "moderate", "Moderado", "Moderate", "中度", "Modéré" -> 1.55
        "active", "Activo", "Active", "活跃", "Actif", "Ativo" -> 1.725
        "very_active", "Muy activo", "Very active", "非常活跃", "Très actif", "Muito ativo" -> 1.9
        else -> 1.4
    }

    private fun goalCalorieAdjustment(goalKey: String): Int = when (goalKey) {
        "lose_weight", "Perder peso", "Lose weight", "减重", "Perdre du poids", "Perder peso" -> -400
        "gain_muscle", "Ganar músculo", "Gain muscle", "增肌", "Gagner du muscle", "Ganhar músculo" -> +300
        "maintain_weight", "Mantener peso", "Maintain weight", "保持体重", "Maintenir le poids", "Manter peso" -> 0
        "improve_health", "Mejorar salud", "Improve health", "改善健康", "Améliorer la santé", "Melhorar saúde" -> -100
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

        // Macros: 30% proteína, 40% carbohidratos, 30% grasa (más proteína si es gain_muscle)
        val proteinPct = if (goalKey.contains("muscle", true) || goalKey.contains("músculo", true)
            || goalKey.contains("肌")) 0.35 else 0.30
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
