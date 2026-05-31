package com.vitaai.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.utils.NutritionCalculator

private const val USERS_COLLECTION = "users"
private const val WEIGHT_FIELD = "weight"
private const val HEIGHT_FIELD = "height"
private const val AGE_FIELD = "age"
private const val ACTIVITY_FIELD = "activity"
private const val GOAL_FIELD = "goal"

private const val DEFAULT_WEIGHT = 70.0
private const val DEFAULT_HEIGHT = 170.0
private const val DEFAULT_AGE = 25
private const val DEFAULT_ACTIVITY = "moderate"
private const val DEFAULT_GOAL = "maintain_weight"

@Composable
fun BMRCalculatorScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var weight by remember { mutableStateOf(0.0) }
    var height by remember { mutableStateOf(0.0) }
    var age by remember { mutableStateOf(0) }
    var activity by remember { mutableStateOf(DEFAULT_ACTIVITY) }
    var goal by remember { mutableStateOf(DEFAULT_GOAL) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection(USERS_COLLECTION).document(uid).get().addOnSuccessListener {
            weight = it.getDouble(WEIGHT_FIELD) ?: DEFAULT_WEIGHT
            height = it.getDouble(HEIGHT_FIELD) ?: DEFAULT_HEIGHT
            age = (it.getLong(AGE_FIELD) ?: DEFAULT_AGE.toLong()).toInt()
            activity = it.getString(ACTIVITY_FIELD) ?: DEFAULT_ACTIVITY
            goal = it.getString(GOAL_FIELD) ?: DEFAULT_GOAL
            ready = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = IconList.Calculate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.bmr_calculator_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))

        if (!ready) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val targets = NutritionCalculator.computeTargets(weight, height, age, activity, goal)
        val bmi = NutritionCalculator.bmi(weight, height)
        val bmiCatKey = NutritionCalculator.bmiCategoryKey(bmi)

        val context = LocalContext.current
        val bmiCatResId = context.resources.getIdentifier(bmiCatKey, "string", context.packageName)
        
        var bmiCatText = bmiCatKey
        if (bmiCatResId != 0) {
            bmiCatText = stringResource(bmiCatResId)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(IconList.Scale, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.bmi_result_format, bmi),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = bmiCatText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(IconList.Fire, null, tint = IconList.FireOrange, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("BMR", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = stringResource(R.string.bmr_unit_format, targets.bmr),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.bmr_explain),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(IconList.Bolt, null, tint = IconList.BoltYellow, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("TDEE", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = stringResource(R.string.bmr_unit_format, targets.tdee),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.tdee_explain),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IconList.Target, null, tint = IconList.TargetRed)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.recommended_targets_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                
                val macroItems: List<Triple<String, String, ImageVector>> = listOf(
                    Triple(stringResource(R.string.calories_label), stringResource(R.string.bmr_unit_format, targets.calories), IconList.Fire),
                    Triple(stringResource(R.string.protein_label), stringResource(R.string.gram_unit_format, targets.proteinG), IconList.Protein),
                    Triple(stringResource(R.string.carbs_label), stringResource(R.string.gram_unit_format, targets.carbsG), IconList.Carbs),
                    Triple(stringResource(R.string.fat_label), stringResource(R.string.gram_unit_format, targets.fatG), IconList.Fat),
                    Triple(stringResource(R.string.water_label), stringResource(R.string.ml_unit_format, targets.waterMl), IconList.Water)
                )

                macroItems.forEach { (label, value, icon) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
