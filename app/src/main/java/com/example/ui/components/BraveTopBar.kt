package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ShieldsState
import com.example.ui.theme.BraveOrange
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.YoutubeRed

@Composable
fun BraveTopBar(
    shieldsState: ShieldsState,
    pageProgress: Int,
    canGoBack: Boolean,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSearchSubmit: (String) -> Unit,
    onShieldsClick: () -> Unit,
    onPipClick: () -> Unit,
    onDesktopToggle: () -> Unit,
    onCompatibilityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSearchActive) {
                // Search Input Mode
                IconButton(
                    onClick = {
                        isSearchActive = false
                        searchQuery = ""
                    },
                    modifier = Modifier.testTag("search_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search YouTube...", fontSize = 14.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                onSearchSubmit(searchQuery.trim())
                                isSearchActive = false
                            }
                        }
                    ),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("search_input_field")
                )

                IconButton(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            onSearchSubmit(searchQuery.trim())
                            isSearchActive = false
                        }
                    },
                    modifier = Modifier.testTag("search_execute_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Execute Search",
                        tint = BraveOrange
                    )
                }
            } else {
                // Standard App Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("app_brand_section")
                ) {
                    if (canGoBack) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("webview_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // BraveTube Brand Emblem
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BraveOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield Emblem",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Brave",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = BraveOrange
                                )
                            )
                            Text(
                                text = "Tube",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = YoutubeRed
                                )
                            )
                        }
                    }
                }

                // Action controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Shields Stat Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (shieldsState.shieldsEnabled) ShieldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onShieldsClick() }
                            .padding(end = 4.dp)
                            .testTag("shields_badge_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shields",
                                tint = if (shieldsState.shieldsEnabled) ShieldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (shieldsState.shieldsEnabled) "${shieldsState.totalBlockedSession}" else "OFF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (shieldsState.shieldsEnabled) ShieldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Search Trigger
                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("search_open_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // PiP Trigger
                    IconButton(
                        onClick = onPipClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Desktop mode toggle
                    IconButton(
                        onClick = onDesktopToggle,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("desktop_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (shieldsState.desktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                            contentDescription = "Toggle Desktop Mode",
                            tint = if (shieldsState.desktopMode) BraveOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Compatibility Matrix & Info
                    IconButton(
                        onClick = onCompatibilityClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("compatibility_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Compatibility Report",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Refresh
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Page",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Web Loading Progress Bar
        AnimatedVisibility(
            visible = pageProgress in 1..99,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { pageProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = BraveOrange,
                trackColor = Color.Transparent
            )
        }
    }
}
