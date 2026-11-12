package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

data class BreadcrumbItem(
    val name: String,
    val file: File,
    val isRoot: Boolean,
    val isCurrent: Boolean,
    val icon: ImageVector
)

@Composable
fun BreadcrumbBar(
    rootDir: File,
    currentDir: File,
    onNavigate: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Build hierarchy from rootDir (or filesystem root) to currentDir
    val breadcrumbs = rememberBreadcrumbHierarchy(rootDir, currentDir)

    // Auto-scroll to end when directory changes
    LaunchedEffect(currentDir.absolutePath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            breadcrumbs.forEachIndexed { index, crumb ->
                BreadcrumbChip(
                    item = crumb,
                    onClick = {
                        if (!crumb.isCurrent) {
                            onNavigate(crumb.file)
                        }
                    }
                )

                if (index < breadcrumbs.lastIndex) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Pemisah",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbChip(
    item: BreadcrumbItem,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            item.isCurrent -> MaterialTheme.colorScheme.primary
            item.isRoot -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "chipColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            item.isCurrent -> MaterialTheme.colorScheme.onPrimary
            item.isRoot -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "textColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        tonalElevation = if (item.isCurrent) 3.dp else 0.5.dp,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

private fun rememberBreadcrumbHierarchy(rootDir: File, currentDir: File): List<BreadcrumbItem> {
    val items = mutableListOf<BreadcrumbItem>()

    // Check if currentDir is inside rootDir
    if (currentDir.absolutePath.startsWith(rootDir.absolutePath)) {
        // Root crumb
        val isAtRoot = currentDir.absolutePath == rootDir.absolutePath
        items.add(
            BreadcrumbItem(
                name = "Penyimpanan Utama",
                file = rootDir,
                isRoot = true,
                isCurrent = isAtRoot,
                icon = Icons.Default.Home
            )
        )

        if (!isAtRoot) {
            val relative = currentDir.absolutePath.removePrefix(rootDir.absolutePath).trimStart('/')
            val segments = relative.split('/')
            var cumulative = rootDir
            for (i in segments.indices) {
                val seg = segments[i]
                if (seg.isNotEmpty()) {
                    cumulative = File(cumulative, seg)
                    val isLast = (i == segments.lastIndex)
                    items.add(
                        BreadcrumbItem(
                            name = seg,
                            file = cumulative,
                            isRoot = false,
                            isCurrent = isLast,
                            icon = Icons.Default.Folder
                        )
                    )
                }
            }
        }
    } else {
        // Outside app root (e.g., Shared storage or SAF)
        val pathSegments = currentDir.absolutePath.split('/').filter { it.isNotEmpty() }
        var cumulative = File("/")
        items.add(
            BreadcrumbItem(
                name = "Root Storage",
                file = cumulative,
                isRoot = true,
                isCurrent = pathSegments.isEmpty(),
                icon = Icons.Default.Storage
            )
        )
        for (i in pathSegments.indices) {
            val seg = pathSegments[i]
            cumulative = File(cumulative, seg)
            val isLast = (i == pathSegments.lastIndex)
            items.add(
                BreadcrumbItem(
                    name = seg,
                    file = cumulative,
                    isRoot = false,
                    isCurrent = isLast,
                    icon = Icons.Default.Folder
                )
            )
        }
    }
    return items
}
