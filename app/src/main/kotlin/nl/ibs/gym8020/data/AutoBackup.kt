package nl.ibs.gym8020.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Automatic backup to a folder the user picked (D-03): after each training the full export is
 * written there as a new file, and only the newest [KEEP] backups are kept. The folder can be in
 * any cloud app that offers one (Google Drive, OneDrive, Nextcloud, …); no account in this app.
 */
class AutoBackup(
    private val context: Context,
    private val backup: BackupService,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()

    /** In the background, e.g. right after finishing a training. No-op when no folder is chosen. */
    fun runSoon() {
        scope.launch { runNow() }
    }

    /** Returns false when no folder is set or writing failed (then the settings show a warning). */
    suspend fun runNow(): Boolean = mutex.withLock {
        val folder = settings.current().backupFolder ?: return false
        val ok = runCatching { write(Uri.parse(folder)) }.isSuccess
        settings.update { it.copy(lastBackupAt = if (ok) System.currentTimeMillis() else it.lastBackupAt, backupFailed = !ok) }
        ok
    }

    /** Stores the chosen folder with lasting access, then makes a first backup. */
    suspend fun setFolder(tree: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(tree, flags)
        settings.update { it.copy(backupFolder = tree.toString(), backupFailed = false) }
        runNow()
    }

    suspend fun clearFolder() {
        val folder = settings.current().backupFolder
        if (folder != null) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(folder), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        settings.update { it.copy(backupFolder = null, lastBackupAt = null, backupFailed = false) }
    }

    private suspend fun write(tree: Uri) {
        val json = backup.export()
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val name = PREFIX + LocalDateTime.now().format(STAMP) + ".json"
            val doc = DocumentsContract.createDocument(resolver, parent, "application/json", name) ?: error("create failed")
            resolver.openOutputStream(doc, "wt")?.use { it.write(json.toByteArray()) } ?: error("no stream")
            runCatching { prune(tree) }
        }
    }

    /** Deletes our older backups; other files in the folder are never touched. */
    private fun prune(tree: Uri) {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val ours = mutableListOf<Pair<String, String>>()
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                if (name.startsWith(PREFIX) && name.endsWith(".json")) ours += c.getString(0) to name
            }
        }
        ours.sortedByDescending { it.second }.drop(KEEP).forEach { (id, _) ->
            DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id))
        }
    }

    companion object {
        const val KEEP = 7
        private const val PREFIX = "gym-8020-backup-"
        private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")

        /** Readable name of a chosen folder, e.g. "Drive/GYM-8020" → "GYM-8020". */
        fun folderLabel(folder: String): String =
            runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(folder)).substringAfterLast(':').substringAfterLast('/') }
                .getOrNull()?.takeIf { it.isNotBlank() } ?: folder
    }
}
