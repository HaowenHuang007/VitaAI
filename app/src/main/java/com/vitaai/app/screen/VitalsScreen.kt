package com.vitaai.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.utils.DateUtils
import com.vitaai.app.utils.StreakManager

data class VitalEntry(
    val date: String,
    val bodyFat: Double?,
    val systolic: Int?,
    val diastolic: Int?,
    val sleepHours: Double?,
    val restingHr: Int?
)

@Composable
fun VitalsScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val context = LocalContext.current

    var bodyFat by remember { mutableStateOf("") }
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var sleep by remember { mutableStateOf("") }
    var hr by remember { mutableStateOf("") }
    var savedMsg by remember { mutableStateOf("") }
    var history by remember { mutableStateOf<List<VitalEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).collection("vitals")
            .orderBy("date", Query.Direction.DESCENDING).limit(30)
            .addSnapshotListener { snap, _ ->
                history = snap?.documents?.map { d ->
                    VitalEntry(
                        date = d.getString("date") ?: "",
                        bodyFat = d.getDouble("bodyFat"),
                        systolic = d.getLong("systolic")?.toInt(),
                        diastolic = d.getLong("diastolic")?.toInt(),
                        sleepHours = d.getDouble("sleepHours"),
                        restingHr = d.getLong("restingHr")?.toInt()
                    )
                } ?: emptyList()
            }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(IconList.Vitals, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_vitals),
                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        Text(stringResource(R.string.vitals_subtitle),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.vitals_log_today),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = bodyFat,
                    onValueChange = { new -> if (new.matches(Regex("^\\d{0,2}([.,]\\d{0,1})?$"))) bodyFat = new.replace(',', '.') },
                    label = { Text(stringResource(R.string.vitals_body_fat)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = systolic,
                        onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 3) systolic = new },
                        label = { Text(stringResource(R.string.vitals_bp_sys)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = diastolic,
                        onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 3) diastolic = new },
                        label = { Text(stringResource(R.string.vitals_bp_dia)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sleep,
                        onValueChange = { new -> if (new.matches(Regex("^\\d{0,2}([.,]\\d{0,1})?$"))) sleep = new.replace(',', '.') },
                        label = { Text(stringResource(R.string.vitals_sleep)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = hr,
                        onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 3) hr = new },
                        label = { Text(stringResource(R.string.vitals_resting_hr)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val data = mutableMapOf<String, Any>("date" to DateUtils.todayKey())
                        bodyFat.toDoubleOrNull()?.let { data["bodyFat"] = it }
                        systolic.toIntOrNull()?.let { data["systolic"] = it }
                        diastolic.toIntOrNull()?.let { data["diastolic"] = it }
                        sleep.toDoubleOrNull()?.let { data["sleepHours"] = it }
                        hr.toIntOrNull()?.let { data["restingHr"] = it }
                        if (data.size <= 1) return@Button
                        db.collection("users").document(uid)
                            .collection("vitals").document(DateUtils.todayKey())
                            .set(data).addOnSuccessListener {
                                savedMsg = context.getString(R.string.common_saved)
                                bodyFat = ""; systolic = ""; diastolic = ""
                                sleep = ""; hr = ""
                                StreakManager.recordActivity()
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.common_save), fontSize = 14.sp) }

                if (savedMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(savedMsg, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.common_recent_history),
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
            Text(stringResource(R.string.vitals_no_data),
                fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        } else {
            history.forEach { v ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(v.date, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        val parts = mutableListOf<String>()
                        v.bodyFat?.let { parts.add("${"%.1f".format(it)}%") }
                        v.systolic?.let { s ->
                            v.diastolic?.let { d -> parts.add("$s/$d") }
                        }
                        v.sleepHours?.let { parts.add("${it}h") }
                        v.restingHr?.let { parts.add("${it} bpm") }
                        Text(parts.joinToString(" · "), fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
