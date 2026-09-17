package graphify.fingerprint

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * Stateless producer of [FileFingerprint] values.
 *
 * The SHA-256 digest is intentionally lazy at call sites: an incremental scan
 * only digests a file once the fast check has failed, keeping the common case
 * I/O-free.
 */
object Fingerprinter {

    fun fingerprint(file: Path): FileFingerprint = FileFingerprint(
        size = Files.size(file),
        mtime = Files.getLastModifiedTime(file).toMillis(),
        sha256 = sha256(file)
    )

    fun sha256(file: Path): String = digest(Files.readAllBytes(file))

    fun sha256(file: File): String = sha256(file.toPath())

    fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
