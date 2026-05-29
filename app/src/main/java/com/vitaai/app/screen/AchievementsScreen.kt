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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.AchievementManager
import com.vitaai.app.utils.LanguageManager

// Constants for UI styling
private const val SCREEN_PADDING = 20
private const val SPACING_SMALL = 8
private const val SPACING_MEDIUM = 12
private const val SPACING_LARGE = 16
private const val SPACING_XLARGE = 32
private const val CARD_CORNER_RADIUS = 20
private const val ITEM_CORNER_RADIUS = 12
private const val PROGRESS_BAR_HEIGHT = 10
private const val PROGRESS_BAR_CORNER_RADIUS = 5

@Composable
fun AchievementsScreen(modifier: Modifier = Modifier) {
    var unlocked by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(Unit) { AchievementManager.loadUnlocked { unlocked = it } }

    val all = AchievementManager.ALL
    val unlockedCount = all.count { it.id in unlocked }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Spacer(Modifier.height(SPACING_SMALL.dp))
        Text(
            text = LanguageManager.t("achievements"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(SPACING_SMALL.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CARD_CORNER_RADIUS.dp),
            colors = CardDefaults.cardColors(containerColor = NavyBlue)
        ) {
            Column(
                modifier = Modifier.padding(SCREEN_PADDING.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$unlockedCount / ${all.size}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                Text(
                    text = LanguageManager.t("unlocked"),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(SPACING_MEDIUM.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PROGRESS_BAR_HEIGHT.dp)
                        .clip(RoundedCornerShape(PROGRESS_BAR_CORNER_RADIUS.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(unlockedCount.toFloat() / all.size.toFloat())
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(PROGRESS_BAR_CORNER_RADIUS.dp))
                            .background(Gold)
                    )
                }
            }
        }

        Spacer(Modifier.height(SPACING_LARGE.dp))

        all.forEach { ach ->
            val isUnlocked = ach.id in unlocked
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(ITEM_CORNER_RADIUS.dp),
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
                    // TODO: The following string contains or is primarily an emoji
                    Text(
                        text = if (isUnlocked) ach.emoji else "🔒",
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.width(SPACING_MEDIUM.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = LanguageManager.t(ach.titleKey),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = LanguageManager.t(ach.descKey),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = "+${ach.xpReward}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) Gold else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        Spacer(Modifier.height(SPACING_XLARGE.dp))
    }
}
