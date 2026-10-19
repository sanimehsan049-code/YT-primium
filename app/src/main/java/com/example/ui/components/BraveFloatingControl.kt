package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ShieldsState
import com.example.ui.theme.BraveOrange
import com.example.ui.theme.ShieldGreen

@Composable
fun BraveFloatingControl(
    shieldsState: ShieldsState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled = shieldsState.shieldsEnabled
    val blockedCount = shieldsState.totalBlockedSession

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(52.dp) // Generous 52dp touch target
            .testTag("brave_floating_control_container")
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color(0xFF1E2026), // High contrast solid graphite
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(
                width = 1.6.dp,
                color = if (isEnabled) BraveOrange else Color(0xFF888888)
            ),
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .testTag("brave_settings_gear_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(42.dp)
            ) {
                // High-visibility Gear / Settings Icon
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Brave YouTube Shields and Settings",
                    tint = if (isEnabled) BraveOrange else Color(0xFFCCCCCC),
                    modifier = Modifier.size(24.dp)
                )

                // Blocked Count Micro Badge
                if (isEnabled && blockedCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(ShieldGreen)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (blockedCount > 99) "99+" else "$blockedCount",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 9.sp
                        )
                    }
                }
            }
        }
    }
}
