package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.SortOption

@Composable
fun QuickSortRow(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onOpenFullSortDialog: () -> Unit,
    totalItems: Int,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Main Sort Selector Chip
        Surface(
            onClick = onOpenFullSortDialog,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Urutkan",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentSort.shortLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Quick Toggle: Name
        QuickFilterChip(
            label = "Nama",
            isSelected = currentSort == SortOption.NAME_ASC || currentSort == SortOption.NAME_DESC,
            suffix = if (currentSort == SortOption.NAME_DESC) "↓" else "↑",
            onClick = {
                val next = if (currentSort == SortOption.NAME_ASC) SortOption.NAME_DESC else SortOption.NAME_ASC
                onSortSelected(next)
            }
        )

        // Quick Toggle: Date
        QuickFilterChip(
            label = "Waktu",
            isSelected = currentSort == SortOption.DATE_DESC || currentSort == SortOption.DATE_ASC,
            suffix = if (currentSort == SortOption.DATE_ASC) "↑" else "↓",
            onClick = {
                val next = if (currentSort == SortOption.DATE_DESC) SortOption.DATE_ASC else SortOption.DATE_DESC
                onSortSelected(next)
            }
        )

        // Quick Toggle: Size
        QuickFilterChip(
            label = "Ukuran",
            isSelected = currentSort == SortOption.SIZE_DESC || currentSort == SortOption.SIZE_ASC,
            suffix = if (currentSort == SortOption.SIZE_ASC) "↑" else "↓",
            onClick = {
                val next = if (currentSort == SortOption.SIZE_DESC) SortOption.SIZE_ASC else SortOption.SIZE_DESC
                onSortSelected(next)
            }
        )

        // Quick Toggle: Type
        QuickFilterChip(
            label = "Tipe",
            isSelected = currentSort == SortOption.TYPE_ASC,
            suffix = "",
            onClick = {
                onSortSelected(SortOption.TYPE_ASC)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Total count badge
        Text(
            text = if (searchQuery.isNotEmpty()) "$totalItems hasil" else "$totalItems item",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(start = 4.dp, end = 6.dp)
        )
    }
}

@Composable
private fun QuickFilterChip(
    label: String,
    isSelected: Boolean,
    suffix: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (suffix.isNotEmpty() && isSelected) "$label $suffix" else label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    onSelectSort: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Urutkan Berkas & Folder",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Pilih kriteria penyusunan berkas di dalam direktori ini.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            SortOptionItem(
                icon = Icons.Default.SortByAlpha,
                title = "Nama (A ke Z)",
                description = "Urutan alfabetis dari A sampai Z",
                isSelected = currentSort == SortOption.NAME_ASC,
                onClick = { onSelectSort(SortOption.NAME_ASC); onDismiss() }
            )

            SortOptionItem(
                icon = Icons.Default.SortByAlpha,
                title = "Nama (Z ke A)",
                description = "Urutan alfabetis terbalik dari Z sampai A",
                isSelected = currentSort == SortOption.NAME_DESC,
                onClick = { onSelectSort(SortOption.NAME_DESC); onDismiss() }
            )

            SortOptionItem(
                icon = Icons.Default.CalendarMonth,
                title = "Terbaru Dimodifikasi",
                description = "Berkas yang baru diedit atau dibuat di posisi teratas",
                isSelected = currentSort == SortOption.DATE_DESC,
                onClick = { onSelectSort(SortOption.DATE_DESC); onDismiss() }
            )

            SortOptionItem(
                icon = Icons.Default.CalendarMonth,
                title = "Terlama Dimodifikasi",
                description = "Berkas paling lama di posisi teratas",
                isSelected = currentSort == SortOption.DATE_ASC,
                onClick = { onSelectSort(SortOption.DATE_ASC); onDismiss() }
            )

            SortOptionItem(
                icon = Icons.Default.DataUsage,
                title = "Ukuran Terbesar",
                description = "Berkas dengan ukuran memori paling besar di atas",
                isSelected = currentSort == SortOption.SIZE_DESC,
                onClick = { onSelectSort(SortOption.SIZE_DESC); onDismiss() }
            )

            SortOptionItem(
                icon = Icons.Default.DataUsage,
                title = "Ukuran Terkecil",
                description = "Berkas dengan ukuran memori paling kecil di atas",
                isSelected = currentSort == SortOption.SIZE_ASC,
                onClick = { onSelectSort(SortOption.SIZE_ASC); onDismiss() }
            )

            SortOptionItem(
                icon = Icons.Default.Category,
                title = "Jenis / Tipe Berkas",
                description = "Kelompokkan folder, zip, gambar, video, dan dokumen teks",
                isSelected = currentSort == SortOption.TYPE_ASC,
                onClick = { onSelectSort(SortOption.TYPE_ASC); onDismiss() }
            )

            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SortOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
    }
}
