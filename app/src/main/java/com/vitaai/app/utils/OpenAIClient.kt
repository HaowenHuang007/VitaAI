package com.vitaai.app.utils

/**
 * AI client: calls a Cloudflare Worker that proxies Google Gemini API.
 *
 * Function names still say "OpenAI" only to keep call sites unchanged across
 * the 7 screens that import them. Under the hood the worker now talks to
 * Gemini 2.0 Flash. See /cloudflare-worker/worker.js for the proxy.
 *
 * Why a proxy? So the API key never ships inside the APK.
 * Auth: every call carries the current Firebase user's ID token.
 */

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val WORKER_BASE_URL = "https://vitaai-proxy.hhw610888.workers.dev"

private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

private suspend fun firebaseIdToken(): String {
    val user = Firebase.auth.currentUser
        ?: throw IllegalStateException("Not signed in")
    return user.getIdToken(false).await().token
        ?: throw IllegalStateException("Failed to obtain Firebase ID token")
}

private suspend fun callWorker(path: String, payload: JSONObject): String {
    val token = firebaseIdToken()
    val request = Request.Builder()
        .url("$WORKER_BASE_URL$path")
        .header("Authorization", "Bearer $token")
        .post(payload.toString().toRequestBody(JSON_MEDIA))
        .build()

    return withContext(Dispatchers.IO) {
        httpClient.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RuntimeException("AI worker error ${resp.code}: ${body.take(300)}")
            }
            JSONObject(body).optString("content", "")
        }
    }
}

suspend fun callOpenAI(prompt: String): String =
    callWorker("/chat", JSONObject().put("prompt", prompt))

// history: list of (role, content) pairs — role is "user" or "assistant"
suspend fun callOpenAIWithHistory(
    systemPrompt: String,
    history: List<Pair<String, String>>
): String {
    val historyJson = JSONArray()
    for ((role, content) in history) {
        historyJson.put(JSONObject().put("role", role).put("content", content))
    }
    return callWorker(
        "/chat-with-history",
        JSONObject().put("systemPrompt", systemPrompt).put("history", historyJson)
    )
}

data class FoodAnalysis(
    val name: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val rating: String,
    val tip: String,
    val displayText: String
)

suspend fun analyzeImageWithOpenAI(
    base64Image: String,
    language: String = "Spanish"
): FoodAnalysis {
    val raw = callWorker(
        "/analyze-image",
        JSONObject().put("base64Image", base64Image).put("language", language)
    )
    val cleaned = raw.trim()
        .removePrefix("```json").removePrefix("```")
        .removeSuffix("```").trim()
    val json = JSONObject(cleaned)
    val name = json.optString("name", "?")
    val calories = json.optInt("calories", 0)
    val protein = json.optInt("protein", 0)
    val carbs = json.optInt("carbs", 0)
    val fat = json.optInt("fat", 0)
    val rating = json.optString("rating", "moderate")
    val tip = json.optString("tip", "")

    val display = buildString {
        append("🍽️ ").append(name).append("\n")
        append("🔥 ").append(calories).append(" kcal\n")
        append("💪 ").append(protein).append("g\n")
        append("🍞 ").append(carbs).append("g\n")
        append("🥑 ").append(fat).append("g\n")
        append("✅ ").append(rating).append("\n")
        if (tip.isNotBlank()) append("💡 ").append(tip)
    }
    return FoodAnalysis(name, calories, protein, carbs, fat, rating, tip, display)
}
