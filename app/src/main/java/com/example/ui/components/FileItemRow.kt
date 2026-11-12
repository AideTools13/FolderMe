package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileItem
import com.example.ui.theme.AudioColor
import com.example.ui.theme.DocumentColor
import com.example.ui.theme.FolderColor
import com.example.ui.theme.ImageColor
import com.example.ui.theme.VideoColor
import com.example.ui.theme.ZipColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onCompress: () -> Unit,
    onExtract: () -> Unit,
    onViewZip: () -> Unit,
    onEditText: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val cardBorder = if (isSelected) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
    } else {
        Modifier
    }

    val cardBg = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .then(cardBorder)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onLongClick()
                    }
                }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox in Multi-Selection Mode
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            // Icon Badge
            val (icon, tint) = getIconAndColor(item)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = if (item.isDirectory) "Folder" else "File",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = item.formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Overflow Menu (Hidden during multi-selection mode)
            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi Berkas",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (item.isZip) {
                            DropdownMenuItem(
                                text = { Text("Pratinjau Isi ZIP") },
                                onClick = {
                                    menuExpanded = false
                                    onViewZip()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ekstrak ZIP") },
                                onClick = {
                                    menuExpanded = false
                                    onExtract()
                                }
                            )
                        }

                        if (!item.isDirectory && (item.extension in listOf("txt", "md", "log", "json", "xml", "csv", ""))) {
                            DropdownMenuItem(
                                text = { Text("Edit Isi Teks") },
                                onClick = {
                                    menuExpanded = false
                                    onEditText()
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Salin (Copy)") },
                            onClick = {
                                menuExpanded = false
                                onCopy()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Pindahkan (Move)") },
                            onClick = {
                                menuExpanded = false
                                onMove()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Kompres ke ZIP") },
                            onClick = {
                                menuExpanded = false
                                onCompress()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Ganti Nama (Rename)") },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )

                        if (!item.isDirectory) {
                            DropdownMenuItem(
                                text = { Text("Bagikan (Share)") },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Detail Berkas") },
                            onClick = {
                                menuExpanded = false
                                onDetails()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Hapus",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getIconAndColor(item: FileItem): Pair<ImageVector, Color> {
    if (item.isDirectory) return Pair(Icons.Default.Folder, FolderColor)
    if (item.isZip) return Pair(Icons.Default.Archive, ZipColor)

    return when (item.extension) {
        "txt", "md", "log", "json", "xml", "csv" -> Pair(Icons.Default.TextSnippet, DocumentColor)
        "jpg", "jpeg", "png", "webp", "gif", "bmp" -> Pair(Icons.Default.Image, ImageColor)
        "mp4", "mkv", "avi", "mov" -> Pair(Icons.Default.Movie, VideoColor)
        "mp3", "wav", "ogg", "flac" -> Pair(Icons.Default.AudioFile, AudioColor)
        else -> Pair(Icons.Default.Description, Color(0xFF78909C))
    }
}
