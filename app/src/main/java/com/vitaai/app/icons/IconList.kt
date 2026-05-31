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
    val ProteinPurple = Color(0xFF9C27B0)
    val CarbsOrange = Color(0xFFFF9800)
    val FatYellow = Color(0xFFFFC107)
    val BoltYellow = Color(0xFFFFEB3B)

    // Mood Colors
    val MoodGreat = Color(0xFF4CAF50)
    val MoodGood = Color(0xFF8BC34A)
    val MoodNormal = Color(0xFF9E9E9E)
    val MoodSad = Color(0xFF2196F3)
    val MoodStressed = Color(0xFFFF9800)
    val MoodTired = Color(0xFF673AB7)
    val MoodIrritable = Color(0xFFF44336)
    val MoodUnwell = Color(0xFF009688)

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

    fun getIconForMood(moodId: String): ImageVector {
        return when (moodId) {
            "great" -> Icons.Filled.SentimentVerySatisfied
            "good" -> Icons.Filled.SentimentSatisfied
            "normal" -> Icons.Filled.SentimentNeutral
            "sad" -> Icons.Filled.SentimentDissatisfied
            "stressed" -> Icons.Filled.Psychology
            "tired" -> Icons.Filled.Bedtime
            "irritable" -> Icons.Filled.SentimentVeryDissatisfied
            "unwell" -> Icons.Filled.Sick
            else -> Icons.Filled.SentimentSatisfied
        }
    }

    fun getColorForMood(moodId: String): Color {
        return when (moodId) {
            "great" -> MoodGreat
            "good" -> MoodGood
            "normal" -> MoodNormal
            "sad" -> MoodSad
            "stressed" -> MoodStressed
            "tired" -> MoodTired
            "irritable" -> MoodIrritable
            "unwell" -> MoodUnwell
            else -> Color.Gray
        }
    }

    fun getIconForLevel(level: Int): ImageVector {
        return when (level) {
            5 -> Icons.Filled.WorkspacePremium
            4 -> Icons.Filled.Star
            3 -> Icons.Filled.FitnessCenter
            2 -> Icons.Filled.Eco
            else -> Icons.Filled.Grass
        }
    }

    fun getIconForExercise(type: String): ImageVector {
        return when (type) {
            "walking" -> Icons.Filled.DirectionsWalk
            "running" -> Icons.Filled.DirectionsRun
            "cycling" -> Icons.Filled.DirectionsBike
            "swimming" -> Icons.Filled.Pool
            "yoga" -> Icons.Filled.SelfImprovement
            "gym" -> Icons.Filled.FitnessCenter
            "dance" -> Icons.Filled.MusicNote
            "hiit" -> Icons.Filled.Whatshot
            else -> Icons.Filled.FitnessCenter
        }
    }

    // Common UI Icons
    val Plan = Icons.Filled.Assignment
    val Progress = Icons.Filled.TrendingUp
    val WeeklyReport = Icons.Filled.Assessment
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
    val Chat = Icons.Filled.Chat
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
    val Tip = Icons.Filled.Lightbulb
    val Mood = Icons.Filled.SentimentSatisfied
    val Calculate = Icons.Filled.Calculate
    val Scale = Icons.Filled.Scale
    val Fire = Icons.Filled.Whatshot
    val Bolt = Icons.Filled.Bolt
    val Target = Icons.Filled.TrackChanges
    val Protein = Icons.Filled.FitnessCenter
    val Carbs = Icons.Filled.BakeryDining
    val Fat = Icons.Filled.BakeryDining
    val Search = Icons.Filled.Search
    val Camera = Icons.Filled.PhotoCamera
    val Info = Icons.Filled.Info
    val Logo = Icons.Filled.Spa
    val Email = Icons.Filled.Email
    val Done = Icons.Filled.CheckCircle
    val Error = Icons.Filled.Error
}
