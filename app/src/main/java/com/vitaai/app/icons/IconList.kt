package com.vitaai.app.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object IconList {
    // Colors
    val WaterBlue = Color(0xFF2196F3)
    val FireOrange = Color(0xFFFF5722)
    val AchievementGold = Color(0xFFFFD700)
    val PlantGreen = Color(0xFF4CAF50)
    val FoodWhite = Color(0xFFF5F5F5)
    val ChatBlue = Color(0xFF007AFF)
    val MoodYellow = Color(0xFFFFEB3B)
    val ExerciseGrey = Color(0xFF757575)
    val TargetRed = Color(0xFFF44336)
    val CameraGrey = Color(0xFF9E9E9E)
    val RobotTeal = Color(0xFF009688)

    fun getIconForAchievement(id: String): ImageVector {
        return when (id) {
            "first_plan" -> Icons.Filled.SmartToy
            "first_food" -> Icons.Filled.PhotoCamera
            "first_chat" -> Icons.Filled.Chat
            "first_mood" -> Icons.Filled.SentimentSatisfied
            "first_water", "water_goal" -> Icons.Filled.WaterDrop
            "first_exercise" -> Icons.Filled.FitnessCenter
            "streak_3", "streak_7", "streak_30" -> Icons.Filled.Whatshot
            "food_10", "food_50" -> Icons.Filled.Restaurant
            "level_3" -> Icons.Filled.Star
            "level_5" -> Icons.Filled.EmojiEvents
            "goal_reached" -> Icons.Filled.TrackChanges
            else -> Icons.Filled.EmojiEvents
        }
    }

    fun getColorForAchievement(id: String): Color {
        return when (id) {
            "first_plan" -> RobotTeal
            "first_food" -> CameraGrey
            "first_chat" -> ChatBlue
            "first_mood" -> MoodYellow
            "first_water", "water_goal" -> WaterBlue
            "first_exercise" -> ExerciseGrey
            "streak_3", "streak_7", "streak_30" -> FireOrange
            "food_10", "food_50" -> FoodWhite
            "level_3", "level_5" -> AchievementGold
            "goal_reached" -> TargetRed
            else -> Color.White
        }
    }

    // Common UI Icons
    val Plan = Icons.Filled.Assignment
    val Progress = Icons.Filled.TrendingUp
    val Nutritionist = Icons.Filled.SmartToy
    val IdentifyFood = Icons.Filled.CameraAlt
    val Level = Icons.Filled.BarChart
    val Settings = Icons.Filled.Settings
    val Logout = Icons.Filled.ExitToApp
    val Update = Icons.Filled.Refresh
    val Diet = Icons.Filled.Restaurant
    val Exercise = Icons.Filled.FitnessCenter
    val Home = Icons.Filled.Home
    val Save = Icons.Filled.Save
    val Language = Icons.Filled.Language
    val Lock = Icons.Filled.Lock
    val Send = Icons.Filled.Send
    val Profile = Icons.Filled.Person
    val Goal = Icons.Filled.Flag
    val Activity = Icons.Filled.DirectionsRun
    val Water = Icons.Filled.WaterDrop
    val History = Icons.Filled.History
    val Achievements = Icons.Filled.EmojiEvents
    val Notification = Icons.Filled.Notifications
    val LightMode = Icons.Filled.LightMode
    val DarkMode = Icons.Filled.DarkMode
    val SystemMode = Icons.Filled.SettingsSuggest
}
