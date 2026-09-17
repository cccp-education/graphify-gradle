package graphify.catalog

/**
 * Publication hygiene verdict for the borough self version.
 *
 * The local catalog version and the *published* workspace catalog version must
 * agree: the ws catalog is the cross-borough source of truth, the local version
 * is what the borough builds. A drift means one of the two was bumped without
 * the other — a broken release chain.
 *
 * This is a pure function so the guard's logic is unit-testable without a
 * filesystem, a Gradle build, or a neighbour repository.
 */
object PublicationHygiene {

    data class Verdict(
        val consistent: Boolean,
        val message: String
    )

    fun check(localVersion: String?, publishedVersion: String?): Verdict {
        if (localVersion.isNullOrBlank()) {
            return Verdict(
                consistent = false,
                message = "local catalog version is missing (expected a graphify self version in [versions])"
            )
        }
        if (publishedVersion.isNullOrBlank()) {
            return Verdict(
                consistent = false,
                message = "published workspace catalog version is missing (was the ws catalog injected at build time?)"
            )
        }
        if (localVersion != publishedVersion) {
            return Verdict(
                consistent = false,
                message = "local version ($localVersion) must match published ws catalog version ($publishedVersion)"
            )
        }
        return Verdict(
            consistent = true,
            message = "local and published versions agree ($localVersion)"
        )
    }
}
