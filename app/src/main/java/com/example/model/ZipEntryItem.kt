package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ZipEntryItem(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean,
    val time: Long
) {
    val simpleName: String
        get() = name.trimEnd('/').substringAfterLast('/')

    val formattedSize: String
        get() = if (isDirectory) "Direktori" else FileItem.formatBytes(size)

    val formattedCompressedSize: String
        get() = if (isDirectory) "-" else FileItem.formatBytes(compressedSize)

    val compressionRatio: String
        get() = if (isDirectory || size <= 0) "-" else {
            val ratio = ((size - compressedSize).toDouble() / size * 100).toInt()
            "${ratio.coerceAtLeast(0)}%"
        }

    val formattedDate: String
        get() {
            if (time <= 0) return "-"
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(Date(time))
        }
}
