package com.vitaai.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.NutritionLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var firestoreError by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualCalories by remember { mutableStateOf("") }
    var manualProtein by remember { mutableStateOf("") }
    var manualCarbs by remember { mutableStateOf("") }
    var manualFat by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).collection("foodlog")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    firestoreError = LanguageManager.t("error_saving") + (error.message ?: "")
                    isLoading = false
                    return@addSnapshotListener
                }
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🍽️ ${LanguageManager.t("food_history")}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAddDialog = true; saveError = "" }) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(4.dp))

            if (firestoreError.isNotEmpty()) {
                Text(
                    firestoreError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

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
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true; saveError = "" },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(LanguageManager.t("add_food")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text(LanguageManager.t("food_name")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualCalories,
                        onValueChange = { manualCalories = it },
                        label = { Text(LanguageManager.t("calories")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualProtein,
                            onValueChange = { manualProtein = it },
                            label = { Text("P (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = manualCarbs,
                            onValueChange = { manualCarbs = it },
                            label = { Text("C (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = manualFat,
                            onValueChange = { manualFat = it },
                            label = { Text("F (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    if (saveError.isNotEmpty()) {
                        Text(saveError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val cal = manualCalories.toIntOrNull() ?: 0
                    val pro = manualProtein.toIntOrNull() ?: 0
                    val car = manualCarbs.toIntOrNull() ?: 0
                    val fat = manualFat.toIntOrNull() ?: 0
                    db.collection("users").document(uid).collection("foodlog")
                        .add(mapOf(
                            "date" to today,
                            "name" to manualName.trim(),
                            "calories" to cal,
                            "proteinG" to pro,
                            "carbsG" to car,
                            "fatG" to fat,
                            "rating" to "moderate",
                            "analysis" to "",
                            "timestamp" to System.currentTimeMillis()
                        ))
                        .addOnSuccessListener {
                            NutritionLog.addMeal(cal, pro, car, fat)
                            showAddDialog = false
                            manualName = ""; manualCalories = ""; manualProtein = ""
                            manualCarbs = ""; manualFat = ""; saveError = ""
                        }
                        .addOnFailureListener { e ->
                            saveError = LanguageManager.t("error_saving") + (e.message ?: "")
                        }
                }) { Text(LanguageManager.t("save")) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(LanguageManager.t("cancel"))
                }
            }
        )
    }
}
