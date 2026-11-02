package nl.ibs.gym8020.data.db

import android.content.Context
import android.util.Log
import java.io.File
import java.io.RandomAccessFile

/**
 * Safety copy before the migration to schema v3 (#11, owner decision Q4): before Room opens a database of an older
 * version, its files (main file, `-wal`, `-shm`, `-journal` when present) are copied to `filesDir/pre-v3/`. Made
 * once and never rotated or deleted by the app.
 *
 * The version is read from the file header, so nothing opens the database before Room does. The header may still
 * show an older version while newer pages sit in the `-wal` file; then a copy is made anyway (at most once).
 */
object PreMigrationCopy {
    const val FOLDER = "pre-v3"

    /** Copies the database files when the database is older than [beforeVersion] and no copy exists yet. */
    fun copyIfNeeded(context: Context, dbName: String, beforeVersion: Int = 3): Boolean =
        copyIfNeeded(context.getDatabasePath(dbName), File(context.filesDir, FOLDER), beforeVersion)

    fun copyIfNeeded(dbFile: File, target: File, beforeVersion: Int = 3): Boolean {
        if (target.exists()) return false
        val version = userVersion(dbFile) ?: return false
        // The header can lag behind the -wal file (0 or an older version until a checkpoint): when in doubt, copy.
        if (version >= beforeVersion) return false
        return runCatching {
            val partial = File(target.parentFile, target.name + ".partial")
            partial.deleteRecursively()
            partial.mkdirs()
            for (suffix in listOf("", "-wal", "-shm", "-journal")) {
                val source = File(dbFile.path + suffix)
                if (source.exists()) source.copyTo(File(partial, source.name), overwrite = true)
            }
            // Only a complete copy gets the final name, so a crash halfway is retried on the next start.
            check(partial.renameTo(target)) { "rename failed" }
            true
        }.getOrElse {
            Log.e("PreMigrationCopy", "copy before migration failed", it)
            false
        }
    }

    /** The SQLite `user_version` (Room's schema version) from the file header, or null without a database. */
    fun userVersion(dbFile: File): Int? {
        if (!dbFile.isFile || dbFile.length() < 100) return null
        return runCatching {
            RandomAccessFile(dbFile, "r").use { f ->
                val magic = ByteArray(16)
                f.readFully(magic)
                if (String(magic, Charsets.US_ASCII) != "SQLite format 3\u0000") return null
                f.seek(60)
                f.readInt()
            }
        }.getOrNull()
    }
}
