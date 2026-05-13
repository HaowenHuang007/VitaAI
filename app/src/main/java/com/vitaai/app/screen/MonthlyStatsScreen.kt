package com.vitaai.app.screen

import android.graphics.Color as AColor
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
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.utils.DateUtils
import com.vitaai.app.utils.LanguageManager
import kotlinx.coroutines.tasks.await

private data class DayStat(
    val date: String,
    val calories: Int,
    val weight: Double,
    val exerciseMin: Int,
    val waterMl: Int
)

@Composable
fun MonthlyStatsScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var stats by remember { mutableStateOf<List<DayStat>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        val from = DateUtils.dateKey(-29)
        val map = mutableMapOf<String, DayStat>()
        for (i in -29..0) {
            val k = DateUtils.dateKey(i)
            map[k] = DayStat(k, 0, 0.0, 0, 0)
        }
        try {
            db.collection("users").document(uid).collection("nutrition")
                .whereGreaterThanOrEqualTo("date", from).get().await()
                .documents.forEach {
                    val d = it.getString("date") ?: return@forEach
                    map[d] = (map[d] ?: DayStat(d, 0, 0.0, 0, 0))
                        .copy(calories = (it.getLong("calories") ?: 0).toInt())
                }
            db.collection("users").document(uid).collection("progress")
                .whereGreaterThanOrEqualTo("date", from).get().await()
                .documents.forEach {
                    val d = it.getString("date") ?: return@forEach
                    map[d] = (map[d] ?: DayStat(d, 0, 0.0, 0, 0))
                        .copy(weight = it.getDouble("weight") ?: 0.0)
                }
            db.collection("users").document(uid).collection("exercises")
                .whereGreaterThanOrEqualTo("date", from).get().await()
                .documents.forEach {
                    val d = it.getString("date") ?: return@forEach
                    val existing = map[d] ?: DayStat(d, 0, 0.0, 0, 0)
                    map[d] = existing.copy(
                        exerciseMin = existing.exerciseMin + (it.getLong("durationMin") ?: 0).toInt()
                    )
                }
            db.collection("users").document(uid).collection("water")
                .whereGreaterThanOrEqualTo("date", from).get().await()
                .documents.forEach {
                    val d = it.getString("date") ?: return@forEach
                    map[d] = (map[d] ?: DayStat(d, 0, 0.0, 0, 0))
                        .copy(waterMl = (it.getLong("ml") ?: 0).toInt())
                }
        } catch (e: Exception) { e.printStackTrace() }
        stats = map.values.sortedBy { it.date }
        loading = false
    }

    val totalCal = stats.sumOf { it.calories }
    val activeDays = stats.count { it.calories > 0 }
    val avgCal = if (activeDays > 0) totalCal / activeDays else 0
    val totalExercise = stats.sumOf { it.exerciseMin }
    val weights = stats.filter { it.weight > 0 }.map { it.weight }
    val weightChange = if (weights.size >= 2) weights.last() - weights.first() else 0.0

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("📅 ${LanguageManager.t("monthly_stats")}",
            fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(LanguageManager.t("last_30_days"),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // KPI tiles
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard("🔥", "$avgCal", LanguageManager.t("avg_calories"), Modifier.weight(1f))
            KpiCard("📅", "$activeDays", LanguageManager.t("active_days"), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard("🏋️", "${totalExercise}min", LanguageManager.t("total_exercise"),
                Modifier.weight(1f))
            KpiCard(
                if (weightChange < 0) "📉" else if (weightChange > 0) "📈" else "➡️",
                "${if (weightChange > 0) "+" else ""}${"%.1f".format(weightChange)}kg",
                LanguageManager.t("weight_change"),
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Calorie bar chart
        Text("🔥 ${LanguageManager.t("daily_calories")}",
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(220.dp).padding(8.dp),
                factory = { ctx ->
                    BarChart(ctx).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setTouchEnabled(true)
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.textColor = AColor.GRAY
                        xAxis.granularity = 1f
                        axisLeft.textColor = AColor.GRAY
                        axisRight.isEnabled = false
                    }
                },
                update = { chart ->
                    val entries = stats.mapIndexed { i, s -> BarEntry(i.toFloat(), s.calories.toFloat()) }
                    val labels = stats.map { it.date.takeLast(5) }
                    val ds = BarDataSet(entries, "kcal").apply {
                        color = AColor.rgb(212, 168, 67)
                        setDrawValues(false)
                    }
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    chart.data = BarData(ds).apply { barWidth = 0.7f }
                    chart.invalidate()
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Weight line chart
        if (weights.size >= 2) {
            Text("⚖️ ${LanguageManager.t("weight_evolution")}",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(8.dp),
                    factory = { ctx ->
                        LineChart(ctx).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(true)
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.textColor = AColor.GRAY
                            xAxis.granularity = 1f
                            axisLeft.textColor = AColor.GRAY
                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        val withWeight = stats.filter { it.weight > 0 }
                        val entries = withWeight.mapIndexed { i, s -> Entry(i.toFloat(), s.weight.toFloat()) }
                        val labels = withWeight.map { it.date.takeLast(5) }
                        val ds = LineDataSet(entries, "kg").apply {
                            color = AColor.rgb(26, 58, 92)
                            setCircleColor(AColor.rgb(212, 168, 67))
                            lineWidth = 2.5f
                            circleRadius = 4f
                            setDrawValues(false)
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                        }
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        chart.data = LineData(ds)
                        chart.invalidate()
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Water bar chart
        Text("💧 ${LanguageManager.t("water")}",
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(8.dp),
                factory = { ctx ->
                    BarChart(ctx).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setTouchEnabled(true)
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.textColor = AColor.GRAY
                        xAxis.granularity = 1f
                        axisLeft.textColor = AColor.GRAY
                        axisRight.isEnabled = false
                    }
                },
                update = { chart ->
                    val entries = stats.mapIndexed { i, s -> BarEntry(i.toFloat(), (s.waterMl / 1000f)) }
                    val labels = stats.map { it.date.takeLast(5) }
                    val ds = BarDataSet(entries, "L").apply {
                        color = AColor.rgb(66, 165, 245)
                        setDrawValues(false)
                    }
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    chart.data = BarData(ds).apply { barWidth = 0.7f }
                    chart.invalidate()
                }
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun KpiCard(emoji: String, value: String, label: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(emoji, fontSize = 22.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
