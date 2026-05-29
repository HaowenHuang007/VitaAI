package com.vitaai.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.AchievementManager

@Composable
fun AchievementsScreen(modifier: Modifier = Modifier) {
    var unlocked by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(Unit) { AchievementManager.loadUnlocked { unlocked = it } }

    val all = AchievementManager.ALL
    val unlockedCount = all.count { it.id in unlocked }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.achievements_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyBlue)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$unlockedCount / ${all.size}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                Text(
                    text = stringResource(R.string.achievements_unlocked_label),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (all.isNotEmpty()) unlockedCount.toFloat() / all.size.toFloat() else 0f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(Gold)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        all.forEach { ach ->
            val isUnlocked = ach.id in unlocked
            
            val titleResId = context.resources.getIdentifier(ach.titleKey, "string", context.packageName)
            val descResId = context.resources.getIdentifier(ach.descKey, "string", context.packageName)
            
            val title = if (titleResId != 0) stringResource(titleResId) else ach.titleKey
            val desc = if (descResId != 0) stringResource(descResId) else ach.descKey

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isUnlocked) IconList.getIconForAchievement(ach.id) else IconList.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (isUnlocked) IconList.getColorForAchievement(ach.id) else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = desc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = stringResource(R.string.common_xp_reward, ach.xpReward),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) Gold else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
