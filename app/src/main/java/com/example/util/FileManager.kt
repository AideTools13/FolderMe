package com.example.util

import android.content.Context
import android.os.Environment
import com.example.model.FileItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

enum class SortOption(val title: String, val shortLabel: String) {
    NAME_ASC("Nama (A - Z)", "Nama A-Z"),
    NAME_DESC("Nama (Z - A)", "Nama Z-A"),
    DATE_DESC("Terbaru Dimodifikasi", "Terbaru"),
    DATE_ASC("Terlama Dimodifikasi", "Terlama"),
    SIZE_DESC("Ukuran Terbesar", "Ukuran ↓"),
    SIZE_ASC("Ukuran Terkecil", "Ukuran ↑"),
    TYPE_ASC("Jenis / Tipe Berkas", "Tipe")
}

object FileManager {

    /**
     * Mengambil direktori awal untuk aplikasi.
     * Menggunakan direktori berkas internal/eksternal aplikasi yang aman dan dapat diakses langsung.
     */
    fun getDefaultRootDirectory(context: Context): File {
        val extDir = context.getExternalFilesDir(null)
        val target = extDir ?: File(context.filesDir, "FolderMeDocs")
        if (!target.exists()) {
            target.mkdirs()
        }
        return target
    }

    /**
     * Mengambil daftar berkas dan folder di direktori saat ini dengan filter dan sorting.
     */
    fun listFiles(
        directory: File,
        searchQuery: String = "",
        sortOption: SortOption = SortOption.NAME_ASC
    ): List<FileItem> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()

        val files = directory.listFiles() ?: return emptyList()

        var items = files.map { FileItem(it) }

        if (searchQuery.isNotBlank()) {
            items = items.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        return when (sortOption) {
            SortOption.NAME_ASC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
            SortOption.NAME_DESC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() })
            SortOption.DATE_DESC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified })
            SortOption.DATE_ASC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.lastModified })
            SortOption.SIZE_DESC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size })
            SortOption.SIZE_ASC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.size })
            SortOption.TYPE_ASC -> items.sortedWith(
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.extension }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    /**
     * Membuat folder baru.
     */
    fun createFolder(parent: File, name: String): Boolean {
        val newFolder = File(parent, name)
        if (newFolder.exists()) return false
        return newFolder.mkdirs()
    }

    /**
     * Membuat file teks baru dengan konten opsional.
     */
    fun createTextFile(parent: File, name: String, initialContent: String = ""): Boolean {
        val fileName = if (name.endsWith(".txt", ignoreCase = true)) name else "$name.txt"
        val newFile = File(parent, fileName)
        if (newFile.exists()) return false
        return try {
            newFile.writeText(initialContent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Membaca isi file teks.
     */
    fun readTextFile(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            "Gagal membaca file: ${e.localizedMessage}"
        }
    }

    /**
     * Menyimpan/mengubah isi file teks.
     */
    fun saveTextFile(file: File, content: String): Boolean {
        return try {
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Mengganti nama file atau folder.
     */
    fun renameItem(file: File, newName: String): Boolean {
        val dest = File(file.parentFile, newName)
        if (dest.exists()) return false
        return file.renameTo(dest)
    }

    /**
     * Menghapus file atau folder secara rekursif.
     */
    fun deleteItemRecursively(file: File): Boolean {
        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    /**
     * Menyalin file atau folder secara rekursif dengan pelaporan progres.
     */
    fun copyItem(
        source: File,
        targetDir: File,
        onProgress: (Int, String) -> Unit
    ): Boolean {
        return try {
            val dest = File(targetDir, source.name)
            if (dest.absolutePath == source.absolutePath) {
                // Jangan salin ke dirinya sendiri
                return false
            }
            if (source.isDirectory) {
                copyDirectoryRecursive(source, dest, onProgress)
            } else {
                onProgress(50, source.name)
                source.copyTo(dest, overwrite = true)
            }
            onProgress(100, "Selesai")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyDirectoryRecursive(source: File, target: File, onProgress: (Int, String) -> Unit) {
        if (!target.exists()) {
            target.mkdirs()
        }
        val items = source.listFiles() ?: return
        for (item in items) {
            val destItem = File(target, item.name)
            onProgress(50, item.name)
            if (item.isDirectory) {
                copyDirectoryRecursive(item, destItem, onProgress)
            } else {
                item.copyTo(destItem, overwrite = true)
            }
        }
    }

    /**
     * Memindahkan file atau folder (Copy lalu Delete).
     */
    fun moveItem(
        source: File,
        targetDir: File,
        onProgress: (Int, String) -> Unit
    ): Boolean {
        val dest = File(targetDir, source.name)
        if (dest.absolutePath == source.absolutePath) return false

        // Coba renameTo terlebih dahulu (cepat jika dalam partisi sama)
        if (source.renameTo(dest)) {
            onProgress(100, "Berhasil dipindahkan")
            return true
        }

        // Jika renameTo gagal (beda mount), lakukan copy kemudian hapus
        val copied = copyItem(source, targetDir, onProgress)
        if (copied) {
            deleteItemRecursively(source)
            return true
        }
        return false
    }

    /**
     * Menginisialisasi data sampel saat pertama kali aplikasi dibuka agar pengguna
     * dapat langsung mencoba fitur CRUD, Teks Editor, dan ZIP Viewer.
     */
    fun seedSampleDataIfEmpty(root: File) {
        val contents = root.listFiles()
        if (contents != null && contents.isNotEmpty()) return

        try {
            // 1. Dokumen Proyek
            val docsFolder = File(root, "Dokumen Proyek")
            docsFolder.mkdirs()

            val catatanFile = File(docsFolder, "catatan_desain.txt")
            catatanFile.writeText(
                "=== Catatan Desain FolderMe ===\n\n" +
                "Tema: Emerald Green (Zamrud)\n" +
                "Arsitektur: File CRUD, Zip Viewer & Archiver, SAF\n" +
                "Fitur unggulan:\n" +
                "- Pratinjau isi ZIP tanpa diekstrak\n" +
                "- Editor teks langsung di dalam aplikasi\n" +
                "- Pengelolaan berkas cepat dan aman"
            )

            val readmeFile = File(root, "panduan_pengguna.txt")
            readmeFile.writeText(
                "Selamat datang di FolderMe!\n\n" +
                "FolderMe adalah pengelola berkas modern dengan fitur:\n" +
                "1. Operasi CRUD: Buat folder, buat file teks, edit isi teks, rename, dan hapus berkas.\n" +
                "2. ZIP Utility: Klik berkas .zip untuk melihat isinya tanpa ekstrak, atau pilih opsi ekstrak.\n" +
                "3. Kompresi ZIP: Buat arsip .zip dari folder atau berkas apa pun.\n" +
                "4. Copy & Move dengan status background.\n" +
                "5. Berbagi berkas langsung via Android Share Sheet."
            )

            // 2. Folder Arsip dengan contoh ZIP
            val arsipFolder = File(root, "Arsip")
            arsipFolder.mkdirs()

            val sampleFilesToZip = listOf(
                File(docsFolder, "catatan_desain.txt")
            )
            val demoZip = File(arsipFolder, "arsip_contoh.zip")
            ZipUtils.createZip(sampleFilesToZip, demoZip) { _, _ -> }

            // 3. Folder Foto & Media
            val mediaFolder = File(root, "Media")
            mediaFolder.mkdirs()
            File(mediaFolder, "info_koleksi.txt").writeText("Direktori untuk menyimpan foto, audio, dan video.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
