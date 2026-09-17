package graphify.fingerprint

/**
 * Cheap identity of a file at a point in time.
 *
 * [size] and [mtime] support a constant-time *fast check*; [sha256] is the
 * content identity used as the extraction cache key, so a file whose content
 * is unchanged but whose mtime moved (git checkout, tar restore) still reuses
 * its previous parse.
 */
data class FileFingerprint(
    val size: Long,
    val mtime: Long,
    val sha256: String
) {
    fun matches(size: Long, mtime: Long): Boolean = this.size == size && this.mtime == mtime
}
