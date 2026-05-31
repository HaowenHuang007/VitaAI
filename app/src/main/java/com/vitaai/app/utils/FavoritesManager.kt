package com.vitaai.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FavoritesManager {

    data class FavoriteFood(
        val id: String,
        val name: String,
        val calories: Int,
        val proteinG: Int,
        val carbsG: Int,
        val fatG: Int,
        val usedCount: Int
    )

    fun add(name: String, cal: Int, p: Int, c: Int, f: Int, onDone: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("favorites")
            .add(mapOf(
                "name" to name,
                "calories" to cal,
                "proteinG" to p,
                "carbsG" to c,
                "fatG" to f,
                "usedCount" to 1,
                "createdAt" to System.currentTimeMillis()
            )).addOnSuccessListener { onDone() }
    }

    fun loadAll(onResult: (List<FavoriteFood>) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("favorites")
            .orderBy("usedCount", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                onResult(snap.documents.map { d ->
                    FavoriteFood(
                        id = d.id,
                        name = d.getString("name") ?: "",
                        calories = (d.getLong("calories") ?: 0).toInt(),
                        proteinG = (d.getLong("proteinG") ?: 0).toInt(),
                        carbsG = (d.getLong("carbsG") ?: 0).toInt(),
                        fatG = (d.getLong("fatG") ?: 0).toInt(),
                        usedCount = (d.getLong("usedCount") ?: 0).toInt()
                    )
                })
            }
    }

    fun delete(id: String, onDone: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("favorites").document(id).delete()
            .addOnSuccessListener { onDone() }
    }

    fun logUse(fav: FavoriteFood, onDone: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid)
            .collection("favorites").document(fav.id)
            .update("usedCount", fav.usedCount + 1)

        db.collection("users").document(uid).collection("foodlog")
            .add(mapOf(
                "date" to DateUtils.todayKey(),
                "name" to fav.name,
                "calories" to fav.calories,
                "proteinG" to fav.proteinG,
                "carbsG" to fav.carbsG,
                "fatG" to fav.fatG,
                "rating" to "moderate",
                "fromFavorite" to true,
                "timestamp" to System.currentTimeMillis()
            )).addOnSuccessListener {
                NutritionLog.addMeal(fav.calories, fav.proteinG, fav.carbsG, fav.fatG)
                onDone()
            }
    }
}
