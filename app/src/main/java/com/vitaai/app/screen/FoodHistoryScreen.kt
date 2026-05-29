package com.vitaai.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val USERS_COLLECTION = "users"
private const val FOODLOG_COLLECTION = "foodlog"
private const val FIELD_DATE = "date"
private const val FIELD_TIMESTAMP = "timestamp"

data class FoodEntry(
    val id: String,
    val date: String,
    val name: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val rating: String
)

@Composable
fun FoodHistoryScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val defaultItemName = stringResource(R.string.food_history_default_item)
    val defaultItem = FoodEntry(
        id = "mock_1",
        date = today,
        name = defaultItemName,
        calories = 420,
        proteinG = 35,
        carbsG = 8,
        fatG = 22,
        rating = "healthy"
    )

    var entries by remember { mutableStateOf<List<FoodEntry>>(listOf(defaultItem)) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        db.collection(USERS_COLLECTION).document(uid)
            .collection(FOODLOG_COLLECTION)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val firestoreEntries = snap?.documents?.map { d ->
                    FoodEntry(
                        id = d.id,
                        date = d.getString(FIELD_DATE) ?: "",
                        name = d.getString("name") ?: "",
                        calories = (d.getLong("calories") ?: 0).toInt(),
                        proteinG = (d.getLong("proteinG") ?: 0).toInt(),
                        carbsG = (d.getLong("carbsG") ?: 0).toInt(),
                        fatG = (d.getLong("fatG") ?: 0).toInt(),
                        rating = d.getString("rating") ?: ""
                    )
                } ?: emptyList()
                
                entries = listOf(defaultItem) + firestoreEntries
                isLoading = false
            }
    }

    val grouped = entries.groupBy { it.date }.toSortedMap(compareByDescending { it })

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = IconList.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.food_history_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty() && !isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.food_history_no_data), color = MaterialTheme.colorScheme.outline)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            grouped.forEach { (date, items) ->
                item {
                    Text(
                        text = date,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(items) { entry ->
                    FoodHistoryItem(entry)
                }
            }
        }
    }
}

@Composable
fun FoodHistoryItem(entry: FoodEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (entry.rating == "healthy") Color(0xFF4CAF50) else Color(0xFFFFC107))
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = stringResource(
                        R.string.food_history_item_macros_format,
                        entry.calories,
                        stringResource(R.string.common_kcal_unit),
                        entry.proteinG,
                        entry.carbsG,
                        entry.fatG
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = { /* Lógica de borrado si se requiere */ }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
