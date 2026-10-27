package com.example.medpulse.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Dashboard", Icons.Filled.Timer, Icons.Outlined.Timer),
    TOPICS("topics", "Planner", Icons.Filled.Assignment, Icons.Outlined.Assignment),
    NOTES("notes", "Notes", Icons.Filled.Description, Icons.Outlined.Description),
    HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History)
}
