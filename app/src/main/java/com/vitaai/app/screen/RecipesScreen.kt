package com.vitaai.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.callOpenAI
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class Recipe(
    val name: String,
    val time: String,
    val difficulty: String,
    val calories: Int,
    val ingredients: List<String>,
    val steps: List<String>
)

@Composable
fun RecipesScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var ingredients by remember { mutableStateOf("") }
    var recipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var expandedIdx by remember { mutableStateOf(-1) }

    val langName = when (LanguageManager.currentLanguage.value) {
        "en" -> "English"; "zh" -> "Chinese"; "fr" -> "French"; "pt" -> "Portuguese"
        else -> "Spanish"
    }

    suspend fun generate() {
        val prompt = """
            You are a healthy cooking expert. Given the available ingredients below,
            propose 3 healthy, easy recipes. Respond in $langName. STRICT JSON only:
            {
              "recipes": [
                {
                  "name": "...",
                  "time": "20 min",
                  "difficulty": "easy|medium|hard",
                  "calories": 450,
                  "ingredients": ["..."],
                  "steps": ["step 1", "step 2", ...]
                }
              ]
            }

            AVAILABLE INGREDIENTS: $ingredients
            Prefer recipes that use mostly these ingredients. It's OK to add common pantry items.
        """.trimIndent()

        val raw = callOpenAI(prompt)
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val arr: JSONArray = JSONObject(cleaned).getJSONArray("recipes")
        val parsed = mutableListOf<Recipe>()
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            val ing = mutableListOf<String>()
            val ingArr = r.optJSONArray("ingredients")
            if (ingArr != null) for (j in 0 until ingArr.length()) ing.add(ingArr.getString(j))
            val steps = mutableListOf<String>()
            val stepArr = r.optJSONArray("steps")
            if (stepArr != null) for (j in 0 until stepArr.length()) steps.add(stepArr.getString(j))
            parsed.add(
                Recipe(
                    name = r.optString("name"),
                    time = r.optString("time"),
                    difficulty = r.optString("difficulty"),
                    calories = r.optInt("calories"),
                    ingredients = ing,
                    steps = steps
                )
            )
        }
        recipes = parsed
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("📖 ${LanguageManager.t("recipes")}",
            fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(LanguageManager.t("recipes_subtitle"),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = ingredients,
            onValueChange = { ingredients = it },
            label = { Text(LanguageManager.t("available_ingredients")) },
            placeholder = { Text(LanguageManager.t("ingredients_example")) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            shape = RoundedCornerShape(12.dp),
            minLines = 2,
            maxLines = 5
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (ingredients.isBlank()) return@Button
                loading = true; recipes = emptyList(); errorMsg = ""; expandedIdx = -1
                scope.launch {
                    try { generate() } catch (e: Exception) {
                        errorMsg = LanguageManager.t("chat_error") + ": ${e.message}"
                    }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading && ingredients.isNotBlank()
        ) {
            Text(
                if (loading) LanguageManager.t("generating_recipes")
                else LanguageManager.t("generate_recipes"),
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error)
        }

        recipes.forEachIndexed { idx, r ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(r.name, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text(
                        "⏱️ ${r.time}  ·  🔥 ${r.calories} kcal  ·  ${difficultyEmoji(r.difficulty)} ${LanguageManager.t("diff_" + r.difficulty)}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
                    if (expandedIdx == idx) {
                        Text("🛒 ${LanguageManager.t("ingredients")}:",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        r.ingredients.forEach { Text("• $it", fontSize = 13.sp, lineHeight = 20.sp) }
                        Spacer(Modifier.height(8.dp))
                        Text("👨‍🍳 ${LanguageManager.t("steps")}:",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        r.steps.forEachIndexed { i, s ->
                            Text("${i + 1}. $s", fontSize = 13.sp, lineHeight = 20.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { expandedIdx = -1 },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(LanguageManager.t("collapse"), fontSize = 13.sp) }
                    } else {
                        Button(
                            onClick = { expandedIdx = idx },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(LanguageManager.t("view_recipe"), fontSize = 13.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

private fun difficultyEmoji(d: String): String = when (d.lowercase()) {
    "easy" -> "🟢"; "hard" -> "🔴"; else -> "🟡"
}
