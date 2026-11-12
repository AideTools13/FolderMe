package com.example.util

import com.example.model.ZipEntryItem
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    /**
     * Zip Viewer: Menampilkan isi dari file .zip tanpa harus mengekstraknya
     * terlebih dahulu dengan membaca ZipInputStream atau java.util.zip.ZipFile.
     */
    fun viewZip(zipFile: File): List<ZipEntryItem> {
        val list = mutableListOf<ZipEntryItem>()
        if (!zipFile.exists() || !zipFile.canRead()) return list

        try {
            val zf = ZipFile(zipFile)
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                list.add(
                    ZipEntryItem(
                        name = entry.name,
                        size = entry.size.coerceAtLeast(0L),
                        compressedSize = entry.compressedSize.coerceAtLeast(0L),
                        isDirectory = entry.isDirectory,
                        time = entry.time
                    )
                )
            }
            zf.close()
        } catch (e: Exception) {
            // Fallback ke ZipInputStream jika ZipFile terhambat
            try {
                ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        list.add(
                            ZipEntryItem(
                                name = entry.name,
                                size = entry.size.coerceAtLeast(0L),
                                compressedSize = entry.compressedSize.coerceAtLeast(0L),
                                isDirectory = entry.isDirectory,
                                time = entry.time
                            )
                        )
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } catch (ignored: Exception) {
            }
        }
        return list.sortedWith(compareBy<ZipEntryItem> { !it.isDirectory }.thenBy { it.name })
    }

    /**
     * Zip Archiver: Mengompresi file atau kumpulan file/folder ke target .zip
     * lengkap dengan callback progress.
     */
    fun createZip(
        sources: List<File>,
        targetZip: File,
        onProgress: (Int, String) -> Unit
    ): Boolean {
        return try {
            val totalFiles = countFiles(sources)
            var processed = 0

            FileOutputStream(targetZip).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    ZipOutputStream(bos).use { zos ->
                        for (source in sources) {
                            processed = compressFileInternal(source, "", zos, totalFiles, processed, onProgress)
                        }
                    }
                }
            }
            onProgress(100, "Selesai")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun compressFileInternal(
        file: File,
        basePath: String,
        zos: ZipOutputStream,
        totalFiles: Int,
        processedCount: Int,
        onProgress: (Int, String) -> Unit
    ): Int {
        var count = processedCount
        val entryName = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"

        if (file.isDirectory) {
            val dirEntry = ZipEntry("$entryName/")
            dirEntry.time = file.lastModified()
            zos.putNextEntry(dirEntry)
            zos.closeEntry()

            val children = file.listFiles() ?: arrayOf()
            for (child in children) {
                count = compressFileInternal(child, entryName, zos, totalFiles, count, onProgress)
            }
        } else {
            val progressPercent = if (totalFiles > 0) ((count.toDouble() / totalFiles) * 100).toInt() else 0
            onProgress(progressPercent, file.name)

            val zipEntry = ZipEntry(entryName)
            zipEntry.time = file.lastModified()
            zos.putNextEntry(zipEntry)

            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var length: Int
                while (fis.read(buffer).also { length = it } > 0) {
                    zos.write(buffer, 0, length)
                }
            }
            zos.closeEntry()
            count++
        }
        return count
    }

    private fun countFiles(files: List<File>): Int {
        var count = 0
        for (f in files) {
            if (f.isDirectory) {
                count += countFiles(f.listFiles()?.toList() ?: emptyList())
            } else {
                count++
            }
        }
        return count.coerceAtLeast(1)
    }

    /**
     * Zip Extractor: Mengekstrak file .zip ke direktori target dengan proteksi Zip Slip.
     */
    fun extractZip(
        zipFile: File,
        targetDir: File,
        onProgress: (Int, String) -> Unit
    ): Boolean {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        return try {
            val zf = ZipFile(zipFile)
            val totalEntries = zf.size()
            var current = 0

            val entries = zf.entries()
            val canonicalDestDir = targetDir.canonicalPath

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val destFile = File(targetDir, entry.name)
                val canonicalDestFile = destFile.canonicalPath

                // Zip Slip vulnerability protection: pastikan file diekstrak di dalam direktori tujuan
                if (!canonicalDestFile.startsWith(canonicalDestDir + File.separator) && canonicalDestFile != canonicalDestDir) {
                    throw SecurityException("Zip Slip detected: ${entry.name}")
                }

                current++
                val percent = if (totalEntries > 0) ((current.toDouble() / totalEntries) * 100).toInt() else 0
                onProgress(percent, entry.name)

                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    zf.getInputStream(entry).use { input ->
                        FileOutputStream(destFile).use { output ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (input.read(buffer).also { len = it } > 0) {
                                output.write(buffer, 0, len)
                            }
                        }
                    }
                }
            }
            zf.close()
            onProgress(100, "Ekstraksi Selesai")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
