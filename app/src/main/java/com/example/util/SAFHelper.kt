package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log

object SAFHelper {
    private const val TAG = "SAFHelper"
    private const val PREFS_NAME = "folderme_saf_prefs"
    private const val KEY_SAF_URI = "saved_saf_tree_uri"

    /**
     * Membuat Intent untuk meminta akses ke pohon direktori (SD Card/External Storage)
     * via Storage Access Framework (SAF).
     */
    fun createOpenDocumentTreeIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        return intent
    }

    /**
     * Menyimpan Uri persistable setelah user memberikan izin melalui dialog SAF.
     */
    fun savePersistedUri(context: Context, treeUri: Uri) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SAF_URI, treeUri.toString()).apply()
            Log.d(TAG, "Persisted SAF tree Uri: $treeUri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist SAF Uri permission", e)
        }
    }

    /**
     * Mengambil Uri yang telah tersimpan sebelumnya jika masih valid.
     */
    fun getSavedTreeUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(KEY_SAF_URI, null) ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            val persistedList = context.contentResolver.persistedUriPermissions
            val hasPerm = persistedList.any { it.uri == uri && it.isWritePermission }
            if (hasPerm) uri else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Membersihkan izin SAF yang tersimpan.
     */
    fun clearSavedTreeUri(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SAF_URI).apply()
    }
}
