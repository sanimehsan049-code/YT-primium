package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BraveOrange
import com.example.ui.theme.ShieldGreen

sealed class NavItem(
    val title: String,
    val icon: ImageVector,
    val url: String?,
    val tag: String
) {
    data object Home : NavItem("Home", Icons.Default.Home, "https://m.youtube.com", "nav_home")
    data object Trending : NavItem("Explore", Icons.Default.Whatshot, "https://m.youtube.com/feed/explore", "nav_trending")
    data object Subscriptions : NavItem("Subs", Icons.Default.Subscriptions, "https://m.youtube.com/feed/subscriptions", "nav_subs")
    data object Library : NavItem("Library", Icons.Default.VideoLibrary, "https://m.youtube.com/feed/library", "nav_library")
    data object Shields : NavItem("Shields", Icons.Default.Security, null, "nav_shields")
}

@Composable
fun BraveBottomNav(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    onShieldsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem.Home,
        NavItem.Trending,
        NavItem.Subscriptions,
        NavItem.Library,
        NavItem.Shields
    )

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface)
            .testTag("brave_bottom_navigation"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        items.forEach { item ->
            val isSelected = selectedItem == item
            val isShieldItem = item == NavItem.Shields

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (isShieldItem) {
                        onShieldsClicked()
                    } else {
                        onItemSelected(item)
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = when {
                            isShieldItem -> ShieldGreen
                            isSelected -> BraveOrange
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        color = when {
                            isShieldItem -> ShieldGreen
                            isSelected -> BraveOrange
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = when {
                        isShieldItem -> ShieldGreen.copy(alpha = 0.15f)
                        else -> BraveOrange.copy(alpha = 0.15f)
                    }
                ),
                modifier = Modifier.testTag(item.tag)
            )
        }
    }
}
