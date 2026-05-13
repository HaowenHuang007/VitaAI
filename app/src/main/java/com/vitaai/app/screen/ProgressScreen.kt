package com.vitaai.app.screen

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.XPManager
import java.text.SimpleDateFormat
import java.util.*

data class ProgressEntry(
    val date: String = "",
    val weight: Double = 0.0,
    val calories: Int = 0
)

@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var weightInput by remember { mutableStateOf("") }
    var caloriesInput by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<ProgressEntry>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }
    var savedMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid)
            .collection("progress")
            .orderBy("date")
            .limit(14)
            .addSnapshotListener { snap, _ ->
                entries = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(ProgressEntry::class.java)
                } ?: emptyList()
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("📊 ${LanguageManager.t("my_progress")}", fontSize = 26.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(LanguageManager.t("today_log"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { new -> if (new.matches(Regex("^\\d{0,3}([.,]\\d{0,2})?$"))) weightInput = new.replace(',', '.') },
                        label = { Text(LanguageManager.t("weight")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = caloriesInput,
                        onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 5) caloriesInput = new },
                        label = { Text(LanguageManager.t("calories")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        isSaving = true
                        db.collection("users").document(uid)
                            .collection("progress").document(today)
                            .set(hashMapOf(
                                "date" to today,
                                "weight" to (weightInput.toDoubleOrNull() ?: 0.0),
                                "calories" to (caloriesInput.toIntOrNull() ?: 0)
                            ))
                            .addOnSuccessListener {
                                isSaving = false
                                // XP con límite diario
                                XPManager.addXPWithLimit(
                                    amount = XPManager.XP_DAILY_LOG,
                                    actionKey = XPManager.KEY_PROGRESS,
                                    dailyLimit = 1
                                ) { added ->
                                    savedMsg = if (added > 0)
                                        "✅ ${LanguageManager.t("saved")} · +${added} XP 🏆"
                                    else
                                        "✅ ${LanguageManager.t("saved")} (${LanguageManager.t("xp_already_claimed_today")})"
                                }
                                weightInput = ""
                                caloriesInput = ""
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving && weightInput.isNotEmpty()
                ) {
                    Text(if (isSaving) LanguageManager.t("saving") else LanguageManager.t("save_today"))
                }
                if (savedMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(savedMsg, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (entries.isNotEmpty()) {
            Text("⚖️ ${LanguageManager.t("weight_evolution")}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(8.dp),
                    factory = { context ->
                        LineChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(false)
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                valueFormatter = IndexAxisValueFormatter(entries.map { it.date.takeLast(5) })
                                granularity = 1f
                                textColor = Color.GRAY
                            }
                            axisLeft.textColor = Color.GRAY
                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        val dataset = LineDataSet(
                            entries.mapIndexed { i, e -> Entry(i.toFloat(), e.weight.toFloat()) },
                            LanguageManager.t("weight_short")
                        ).apply {
                            color = Color.rgb(26, 58, 92)
                            setCircleColor(Color.rgb(212, 168, 67))
                            lineWidth = 2f
                            circleRadius = 4f
                            setDrawValues(true)
                            valueTextColor = Color.GRAY
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                        }
                        chart.data = LineData(dataset)
                        chart.invalidate()
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("🔥 ${LanguageManager.t("daily_calories")}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(8.dp),
                    factory = { context ->
                        LineChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(true)
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                valueFormatter = IndexAxisValueFormatter(entries.map { it.date.takeLast(5) })
                                granularity = 1f
                                textColor = Color.GRAY
                            }
                            axisLeft.textColor = Color.GRAY
                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        val dataset = LineDataSet(
                            entries.mapIndexed { i, e -> Entry(i.toFloat(), e.calories.toFloat()) },
                            LanguageManager.t("calories")
                        ).apply {
                            color = Color.rgb(212, 168, 67)
                            setCircleColor(Color.rgb(212, 168, 67))
                            lineWidth = 2f
                            circleRadius = 4f
                            setDrawValues(true)
                            valueTextColor = Color.GRAY
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                        }
                        chart.data = LineData(dataset)
                        chart.invalidate()
                    }
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                contentAlignment = Alignment.Center) {
                Text(LanguageManager.t("no_data_yet"),
                    color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}