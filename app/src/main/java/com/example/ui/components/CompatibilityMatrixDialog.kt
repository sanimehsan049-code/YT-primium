package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BraveOrange
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.YoutubeRed

data class CompatibilityItem(
    val featureName: String,
    val category: String,
    val androidStatus: String, // "Working & Tested", "Supported", "Limited"
    val iosStatus: String,
    val windowsStatus: String,
    val macOsStatus: String,
    val linuxStatus: String,
    val webStatus: String,
    val method: String,
    val limitations: String,
    val isTestedOnAndroid: Boolean
)

val COMPATIBILITY_ITEMS = listOf(
    CompatibilityItem(
        featureName = "Genuine YouTube & Real Account Sign-In",
        category = "Account",
        androidStatus = "Working & Tested",
        iosStatus = "Supported (WKWebView)",
        windowsStatus = "Supported (WebView2)",
        macOsStatus = "Supported (WebKit)",
        linuxStatus = "Supported (WebKitGTK)",
        webStatus = "Native",
        method = "Direct YouTube Web with modern sanitized User-Agent and full cookie persistence.",
        limitations = "Requires official Google account login; no third-party credential interception.",
        isTestedOnAndroid = true
    ),
    CompatibilityItem(
        featureName = "Brave Shields: Ad & Video Ad Blocking",
        category = "Shields",
        androidStatus = "Working & Tested",
        iosStatus = "Supported (Content Blocker)",
        windowsStatus = "Supported (Brave Engine)",
        macOsStatus = "Supported (Brave Engine)",
        linuxStatus = "Supported (Brave Engine)",
        webStatus = "Supported (Extension)",
        method = "Network interception of ad/telemetry endpoints + injected cosmetic CSS + DOM video ad skipper.",
        limitations = "YouTube frequently experiments with server-side ad stitching, handled via real-time DOM auto-skip.",
        isTestedOnAndroid = true
    ),
    CompatibilityItem(
        featureName = "Background Audio & Screen-Off Playback",
        category = "Playback",
        androidStatus = "Working & Tested",
        iosStatus = "Partial (Audio session lock)",
        windowsStatus = "Supported",
        macOsStatus = "Supported",
        linuxStatus = "Supported",
        webStatus = "Browser Tab active",
        method = "Android Foreground Media Service + MediaSessionCompat + WakeLock + JS Page Visibility bypass.",
        limitations = "iOS requires background audio permission mode. Display off audio works via native service.",
        isTestedOnAndroid = true
    ),
    CompatibilityItem(
        featureName = "Picture-in-Picture (PiP) Floating Window",
        category = "Playback",
        androidStatus = "Working & Tested",
        iosStatus = "Supported (AVPlayer PiP)",
        windowsStatus = "Supported (Compact Overlay)",
        macOsStatus = "Supported (Native PiP)",
        linuxStatus = "Supported (Wayland/X11 Float)",
        webStatus = "Supported (HTML5 PiP API)",
        method = "Android framework PictureInPictureParams with auto-enter on user leave / top bar trigger.",
        limitations = "Requires Android 8.0+ (API 26) and system PiP permission enabled in device settings.",
        isTestedOnAndroid = true
    ),
    CompatibilityItem(
        featureName = "Lock Screen & Headset Media Controls",
        category = "Playback",
        androidStatus = "Working & Tested",
        iosStatus = "Supported (MPNowPlayingInfo)",
        windowsStatus = "Supported (SMTC)",
        macOsStatus = "Supported (NowPlaying)",
        linuxStatus = "Supported (MPRIS)",
        webStatus = "Supported (MediaSession API)",
        method = "Android MediaSession with ACTION_PLAY, ACTION_PAUSE, ACTION_REWIND, ACTION_FORWARD actions.",
        limitations = "Requires Android 13+ POST_NOTIFICATIONS permission for media notification display.",
        isTestedOnAndroid = true
    ),
    CompatibilityItem(
        featureName = "Fullscreen & Immersive Mode",
        category = "UI",
        androidStatus = "Working & Tested",
        iosStatus = "Supported (Full window)",
        windowsStatus = "Supported (F11)",
        macOsStatus = "Supported",
        linuxStatus = "Supported",
        webStatus = "Supported (Fullscreen API)",
        method = "WebChromeClient onShowCustomView with immersive sticky system bar hiding.",
        limitations = "Orientation locking can be overridden by system auto-rotate toggle.",
        isTestedOnAndroid = true
    ),
    CompatibilityItem(
        featureName = "Cookie & Premium Upsell Nag Dismissal",
        category = "Shields",
        androidStatus = "Working & Tested",
        iosStatus = "Supported (CSS inject)",
        windowsStatus = "Supported (AdBlock rule)",
        macOsStatus = "Supported (AdBlock rule)",
        linuxStatus = "Supported (AdBlock rule)",
        webStatus = "Supported (Userscript)",
        method = "Injected DOM observer auto-dismisses 'Try Premium' and consent dialogs without blocking video.",
        limitations = "Changes to YouTube's modal element class names require fallback text/attribute selectors.",
        isTestedOnAndroid = true
    ),
    CompatibilityItem(
        featureName = "Desktop vs Mobile View Switcher",
        category = "UI",
        androidStatus = "Working & Tested",
        iosStatus = "Supported (UA override)",
        windowsStatus = "Default (Desktop)",
        macOsStatus = "Default (Desktop)",
        linuxStatus = "Default (Desktop)",
        webStatus = "Native",
        method = "Dynamic UserAgent switching between clean Chrome Mobile and Linux X11 Desktop string.",
        limitations = "Desktop YouTube requires higher memory and network bandwidth on low-end devices.",
        isTestedOnAndroid = true
    ),
    CompatibilityItem(
        featureName = "Offline Downloads (YouTube Premium / DRM)",
        category = "Policy",
        androidStatus = "Platform Restricted",
        iosStatus = "Platform Restricted",
        windowsStatus = "Platform Restricted",
        macOsStatus = "Platform Restricted",
        linuxStatus = "Platform Restricted",
        webStatus = "Platform Restricted",
        method = "App cache only (standard HTTP caching).",
        limitations = "PRD Section 5 & 6 Compliance: Offline video decrypting bypasses YouTube DRM / Terms. In accordance with policy, we do NOT promise unauthorized Premium decryption.",
        isTestedOnAndroid = false
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompatibilityMatrixDialog(
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Playback", "Shields", "Account", "UI", "Policy")

    val filteredItems = remember(selectedCategory) {
        if (selectedCategory == "All") COMPATIBILITY_ITEMS
        else COMPATIBILITY_ITEMS.filter { it.category == selectedCategory }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("compatibility_matrix_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BraveOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Cross-Platform Matrix",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "PRD Section 4 Feature Compatibility",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_matrix_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Filter Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BraveOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Matrix Cards
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems) { item ->
                        CompatibilityCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun CompatibilityCard(item: CompatibilityItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.featureName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        item.isTestedOnAndroid -> ShieldGreen.copy(alpha = 0.15f)
                        item.androidStatus.contains("Restricted") -> YoutubeRed.copy(alpha = 0.15f)
                        else -> BraveOrange.copy(alpha = 0.15f)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isTestedOnAndroid) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (item.isTestedOnAndroid) ShieldGreen else YoutubeRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isTestedOnAndroid) "Android Tested" else "Restricted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isTestedOnAndroid) ShieldGreen else YoutubeRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Platform badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlatformBadge("Android", item.androidStatus)
                PlatformBadge("iOS", item.iosStatus)
                PlatformBadge("Win", item.windowsStatus)
                PlatformBadge("Mac", item.macOsStatus)
                PlatformBadge("Linux", item.linuxStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Method: ${item.method}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.limitations.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Limitations: ${item.limitations}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun PlatformBadge(platform: String, status: String) {
    val isGood = status.contains("Working") || status.contains("Supported") || status.contains("Native")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(platform, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = if (isGood) "✓ Yes" else if (status.contains("Partial")) "~ Part" else "✗ No",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isGood) ShieldGreen else if (status.contains("Partial")) BraveOrange else YoutubeRed
        )
    }
}
