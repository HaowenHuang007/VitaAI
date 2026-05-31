package com.vitaai.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.utils.FavoritesManager
import com.vitaai.app.utils.StreakManager

@Composable
fun FavoritesScreen(modifier: Modifier = Modifier) {
    var favorites by remember { mutableStateOf<List<FavoritesManager.FavoriteFood>>(emptyList()) }
    var showAdd by remember { mutableStateOf(false) }
    var logMsg by remember { mutableStateOf("") }

    fun reload() = FavoritesManager.loadAll { favorites = it }
    LaunchedEffect(Unit) { reload() }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(IconList.Favorites, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_favorites),
                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f))
            Button(onClick = { showAdd = !showAdd }, shape = RoundedCornerShape(12.dp)) {
                Text(if (showAdd) "✕" else "+", fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.favorites_subtitle),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        if (showAdd) {
            AddFavoriteCard(onSaved = { showAdd = false; reload() })
            Spacer(Modifier.height(16.dp))
        }

        if (logMsg.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(logMsg, modifier = Modifier.padding(12.dp), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
        }

        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.favorites_empty),
                    color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
            }
        } else {
            favorites.forEach { fav ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fav.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${fav.calories} kcal · P${fav.proteinG} · C${fav.carbsG} · F${fav.fatG}",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.outline
                            )
                            if (fav.usedCount > 1) {
                                Text("× ${fav.usedCount}", fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                FavoritesManager.logUse(fav) {
                                    StreakManager.recordActivity()
                                    logMsg = "${fav.name} +${fav.calories} kcal"
                                    reload()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) { Text("+ ${stringResource(R.string.favorites_log)}", fontSize = 12.sp) }
                        IconButton(onClick = {
                            FavoritesManager.delete(fav.id) { reload() }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete),
                                tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AddFavoriteCard(onSaved: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var cal by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.favorites_add_title),
                fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.food_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cal,
                    onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 5) cal = new },
                    label = { Text("kcal") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = p,
                    onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 3) p = new },
                    label = { Text("P") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = c,
                    onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 3) c = new },
                    label = { Text("C") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = f,
                    onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 3) f = new },
                    label = { Text("F") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val ci = cal.toIntOrNull() ?: 0
                    val pi = p.toIntOrNull() ?: 0
                    val ck = c.toIntOrNull() ?: 0
                    val fi = f.toIntOrNull() ?: 0
                    if (name.isNotBlank() && ci > 0) {
                        FavoritesManager.add(name.trim(), ci, pi, ck, fi) { onSaved() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && (cal.toIntOrNull() ?: 0) > 0
            ) { Text(stringResource(R.string.common_save), fontSize = 14.sp) }
        }
    }
}
