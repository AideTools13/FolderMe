package com.example.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) (file.listFiles()?.size?.toLong() ?: 0L) else file.length(),
    val lastModified: Long = file.lastModified(),
    val isZip: Boolean = !file.isDirectory && file.name.endsWith(".zip", ignoreCase = true)
) {
    val extension: String
        get() = if (isDirectory) "" else file.extension.lowercase()

    val formattedSize: String
        get() = if (isDirectory) {
            "$size item"
        } else {
            formatBytes(size)
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val mimeType: String
        get() = when (extension) {
            "txt", "md", "log", "json", "xml", "csv", "html" -> "text/plain"
            "pdf" -> "application/pdf"
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> "image/*"
            "mp4", "mkv", "avi", "mov", "3gp" -> "video/*"
            "mp3", "wav", "ogg", "m4a", "flac" -> "audio/*"
            "zip", "rar", "7z", "tar", "gz" -> "application/zip"
            else -> "*/*"
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
            val pre = "KMGTPE"[exp - 1]
            return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
        }
    }
}
