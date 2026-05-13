package com.vitaai.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vitaai.app.utils.LanguageManager

data class FoodEntry(
    val id: String,
    val date: String,
    val name: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val rating: String,
    val analysis: String
)

@Composable
fun FoodHistoryScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var entries by remember { mutableStateOf<List<FoodEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).collection("foodlog")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, _ ->
                entries = snap?.documents?.map { d ->
                    FoodEntry(
                        id = d.id,
                        date = d.getString("date") ?: "",
                        name = d.getString("name") ?: "",
                        calories = (d.getLong("calories") ?: 0).toInt(),
                        proteinG = (d.getLong("proteinG") ?: 0).toInt(),
                        carbsG = (d.getLong("carbsG") ?: 0).toInt(),
                        fatG = (d.getLong("fatG") ?: 0).toInt(),
                        rating = d.getString("rating") ?: "",
                        analysis = d.getString("analysis") ?: ""
                    )
                } ?: emptyList()
                isLoading = false
            }
    }

    val grouped = entries.groupBy { it.date }.toSortedMap(compareByDescending { it })

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("🍽️ ${LanguageManager.t("food_history")}", fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(LanguageManager.t("no_food_yet"),
                    color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                grouped.forEach { (date, items) ->
                    val dayCalories = items.sumOf { it.calories }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Text(date, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f))
                            Text("$dayCalories kcal", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    items(items) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val emoji = when (entry.rating.lowercase()) {
                                    "healthy", "saludable", "sain", "saudável", "健康" -> "🟢"
                                    "avoid", "evitar", "éviter", "避免" -> "🔴"
                                    else -> "🟡"
                                }
                                Text(emoji, fontSize = 22.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        entry.name.ifBlank { "?" },
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${entry.calories} kcal · P${entry.proteinG} · C${entry.carbsG} · F${entry.fatG}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        db.collection("users").document(uid)
                                            .collection("foodlog").document(entry.id).delete()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}
