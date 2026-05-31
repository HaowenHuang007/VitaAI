package com.vitaai.app.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.callOpenAI
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

private data class ShopItem(val name: String, val qty: String, val category: String, val checked: Boolean)

@Composable
fun ShoppingListScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var items by remember { mutableStateOf<List<ShopItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val langName = when (LanguageManager.currentLanguage.value) {
        "en" -> "English"; "zh" -> "Chinese"; "fr" -> "French"; "pt" -> "Portuguese"
        else -> "Spanish"
    }

    suspend fun generate() {
        val doc = db.collection("users").document(uid).get().await()
        val dietPlan = doc.getString("dietPlan") ?: ""
        val goal = doc.getString("goal") ?: "maintain_weight"
        val weight = doc.getDouble("weight") ?: 70.0

        val prompt = """
            You are a meal-planning assistant. Generate a 7-day shopping list for one person.
            Goal: $goal, weight: ${weight}kg.
            User's diet plan context: ${if (dietPlan.length > 600) dietPlan.take(600) else dietPlan}

            Respond in $langName. Return STRICT JSON, no markdown:
            {
              "items": [
                { "name": "...", "qty": "1kg / 6 units / 500ml", "category": "produce|protein|dairy|grains|pantry|other" }
              ]
            }
            Aim for 20-35 items total, balanced and realistic for the goal.
        """.trimIndent()

        val raw = callOpenAI(prompt)
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        val arr: JSONArray = json.getJSONArray("items")
        val parsed = mutableListOf<ShopItem>()
        for (i in 0 until arr.length()) {
            val it = arr.getJSONObject(i)
            parsed.add(
                ShopItem(
                    name = it.optString("name"),
                    qty = it.optString("qty"),
                    category = it.optString("category", "other"),
                    checked = false
                )
            )
        }
        items = parsed
    }

    LaunchedEffect(Unit) {
        loading = true
        try { generate() } catch (e: Exception) {
            errorMsg = context.getString(R.string.chat_error) + ": ${e.message}"
        }
        loading = false
    }

    val grouped = items.groupBy { it.category }
    val checkedCount = items.count { it.checked }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.home_shopping_list),
                    fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                if (items.isNotEmpty()) {
                    Text("$checkedCount / ${items.size}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            if (items.isNotEmpty()) {
                IconButton(onClick = {
                    val text = items.joinToString("\n") { "• ${it.name} (${it.qty})" }
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clip.setPrimaryClip(ClipData.newPlainText("Shopping list", text))
                }) { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = {
                    val text = items.joinToString("\n") { "• ${it.name} (${it.qty})" }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }) { Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.shopping_generating),
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        } else if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error)
        } else {
            grouped.entries.sortedBy { it.key }.forEach { (cat, list) ->
                val catRes = when (cat) {
                    "produce" -> R.string.shop_cat_produce
                    "protein" -> R.string.shop_cat_protein
                    "dairy" -> R.string.shop_cat_dairy
                    "grains" -> R.string.shop_cat_grains
                    "pantry" -> R.string.shop_cat_pantry
                    else -> R.string.shop_cat_other
                }
                Text(stringResource(catRes),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                list.forEach { item ->
                    val idx = items.indexOf(item)
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (item.checked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable {
                                items = items.toMutableList().also { lst ->
                                    if (idx >= 0) lst[idx] = item.copy(checked = !item.checked)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.checked,
                            onCheckedChange = { v ->
                                items = items.toMutableList().also { lst ->
                                    if (idx >= 0) lst[idx] = item.copy(checked = v)
                                }
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.checked) MaterialTheme.colorScheme.outline
                                else MaterialTheme.colorScheme.onSurface)
                            Text(item.qty, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    loading = true; items = emptyList(); errorMsg = ""
                    scope.launch {
                        try { generate() } catch (e: Exception) {
                            errorMsg = context.getString(R.string.chat_error) + ": ${e.message}"
                        }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.common_regenerate), fontSize = 14.sp) }
            Spacer(Modifier.height(32.dp))
        }
    }
}
