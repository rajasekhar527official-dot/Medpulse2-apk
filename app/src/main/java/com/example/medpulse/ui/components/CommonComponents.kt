package com.example.medpulse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BreakMint
import com.example.ui.theme.BreakMintContainer
import com.example.ui.theme.MedTealPrimary
import com.example.ui.theme.NoteColorBlue
import com.example.ui.theme.NoteColorGreen
import com.example.ui.theme.NoteColorPeach
import com.example.ui.theme.NoteColorPink
import com.example.ui.theme.NoteColorPurple
import com.example.ui.theme.NoteColorWhite
import com.example.ui.theme.NoteColorYellow
import com.example.ui.theme.NoteDarkBlue
import com.example.ui.theme.NoteDarkGreen
import com.example.ui.theme.NoteDarkPeach
import com.example.ui.theme.NoteDarkPink
import com.example.ui.theme.NoteDarkPurple
import com.example.ui.theme.NoteDarkYellow
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseCoralContainer
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

object NotePalette {
    val lightColors = listOf(
        NoteColorWhite,
        NoteColorYellow,
        NoteColorPeach,
        NoteColorPink,
        NoteColorGreen,
        NoteColorBlue,
        NoteColorPurple
    )

    val darkColors = listOf(
        Slate900,
        NoteDarkYellow,
        NoteDarkPeach,
        NoteDarkPink,
        NoteDarkGreen,
        NoteDarkBlue,
        NoteDarkPurple
    )

    fun getColor(colorIndex: Int, isDark: Boolean): Color {
        val safeIndex = colorIndex.coerceIn(0, lightColors.size - 1)
        return if (isDark) darkColors[safeIndex] else lightColors[safeIndex]
    }
}

@Composable
fun PriorityBadge(priority: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (priority.lowercase()) {
        "high" -> PulseCoralContainer to PulseCoral
        "medium" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        else -> Color(0xFFE2E8F0) to Slate700
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = priority.uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SubjectChip(
    subject: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "subjectChipBg"
    )
    val textColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "subjectChipText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = subject,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ColorPickerRow(
    selectedIndex: Int,
    isDark: Boolean,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        NotePalette.lightColors.indices.forEach { index ->
            val color = NotePalette.getColor(index, isDark)
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .clickable { onSelectIndex(index) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
