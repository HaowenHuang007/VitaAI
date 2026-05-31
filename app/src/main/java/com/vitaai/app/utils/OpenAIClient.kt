package com.vitaai.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val OPENAI_API_KEY = "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"
private const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"
private const val TIMEOUT_MS = 30_000

private fun openConnection(): HttpURLConnection {
    val connection = URL(OPENAI_URL).openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Authorization", "Bearer $OPENAI_API_KEY")
    connection.connectTimeout = TIMEOUT_MS
    connection.readTimeout = TIMEOUT_MS
    connection.doOutput = true
    return connection
}

private fun HttpURLConnection.readResponse(): String =
    try { inputStream.bufferedReader().readText() }
    catch (e: Exception) { errorStream?.bufferedReader()?.readText() ?: throw e }

private fun String.extractContent(): String =
    JSONObject(this).getJSONArray("choices")
        .getJSONObject(0).getJSONObject("message").getString("content")

suspend fun callOpenAI(prompt: String): String = withContext(Dispatchers.IO) {
    val connection = openConnection()
    val body = JSONObject().apply {
        put("model", "gpt-4o-mini")
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are an expert nutritionist. Always respond in valid JSON. Values must be plain text, never nested JSON.")
            })
            put(JSONObject().apply { put("role", "user"); put("content", prompt) })
        })
        put("max_tokens", 1500)
        put("temperature", 0.7)
    }.toString()
    connection.outputStream.write(body.toByteArray())
    connection.readResponse().extractContent()
}

// history: list of (role, content) pairs — role is "user" or "assistant"
suspend fun callOpenAIWithHistory(systemPrompt: String, history: List<Pair<String, String>>): String =
    withContext(Dispatchers.IO) {
        val connection = openConnection()
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                history.forEach { (role, content) ->
                    put(JSONObject().apply { put("role", role); put("content", content) })
                }
            })
            put("max_tokens", 500)
            put("temperature", 0.7)
        }.toString()
        connection.outputStream.write(body.toByteArray())
        connection.readResponse().extractContent()
    }

data class FoodAnalysis(
    val name: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val rating: String,
    val tip: String
) {
    val displayText: String
        get() = "$name\n$calories kcal | P: ${proteinG}g C: ${carbsG}g F: ${fatG}g\nRating: $rating\n\n$tip"
}

suspend fun analyzeImageWithOpenAI(base64Image: String, language: String = "Spanish"): FoodAnalysis =
    withContext(Dispatchers.IO) {
        val connection = openConnection()
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert nutritionist. Always respond in valid JSON only, no markdown fences. All textual fields must be in $language.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", """
                                Analyze the food in this image. Return strict JSON with these keys:
                                {
                                  "name": "food name",
                                  "calories": integer (kcal for the visible portion),
                                  "protein": integer (grams),
                                  "carbs": integer (grams),
                                  "fat": integer (grams),
                                  "rating": one of "healthy"|"moderate"|"avoid",
                                  "tip": "one brief nutritional tip"
                                }
                                If unsure, give your best estimate. No text outside the JSON.
                            """.trimIndent())
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                    })
                })
            })
            put("response_format", JSONObject().apply { put("type", "json_object") })
            put("max_tokens", 400)
        }.toString()
        connection.outputStream.write(body.toByteArray())
        val raw = connection.readResponse().extractContent()
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        
        FoodAnalysis(
            name = json.optString("name", "?"),
            calories = json.optInt("calories", 0),
            proteinG = json.optInt("protein", 0),
            carbsG = json.optInt("carbs", 0),
            fatG = json.optInt("fat", 0),
            rating = json.optString("rating", "moderate"),
            tip = json.optString("tip", "")
        )
    }
