package graphify

import graphify.fingerprint.ScanCache
import graphify.fingerprint.ScanCacheStore
import graphify.fingerprint.ScanExtractionIndex
import graphify.model.GraphCommunity
import graphify.model.GraphEdge
import graphify.model.GraphModel
import graphify.model.GraphNode
import graphify.schema.EdgeType
import graphify.schema.NodeType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*

@DisableCachingByDefault(because = "File system scan varies between environments")
open class ScanWorkspaceTask : DefaultTask() {

    @get:Internal
    lateinit var rootDir: File

    @get:Internal
    lateinit var outputFile: File

    @get:Internal
    var excludePatterns: List<String> = emptyList()

    @get:Internal
    var incremental: Boolean = false

    @get:Internal
    var cacheFile: File? = null

    private val mapper: ObjectMapper = ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerKotlinModule()

    @TaskAction
    fun scan() {
        val root = rootDir.toPath()
        val output = outputFile

        val matchers = excludePatterns.map { pattern ->
            FileSystems.getDefault().getPathMatcher("glob:$pattern")
        }
        val customExcludedNames = extractExcludedNamesFromPatterns(excludePatterns)

        val allFileData = mutableListOf<FileInfo>()
        try {
            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (isExcluded(dir, root, matchers, customExcludedNames)) return FileVisitResult.SKIP_SUBTREE
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!isExcluded(file, root, matchers, customExcludedNames)) {
                        allFileData.add(FileInfo(file, attrs.size()))
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                    logger.warn("Skipping inaccessible path: $file")
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (e: Exception) {
            logger.warn("Walk error: ${e.message}, continuing with partial results")
        }

        val allFiles = allFileData.map { it.path }

        val projects = allFiles.filter { isProjectMarker(it) }
            .map { it.parent }
            .distinct()
        val projectDirs = projects.toSet()

        val dirs = allFiles.asSequence()
            .map { it.parent }
            .distinct()
            .filter { dir -> allFiles.any { it.parent == dir } }
            .toList()

        val repoMap = buildRepoMap(root, allFiles)
        val nodes = extractNodes(root, allFileData, dirs, projects, repoMap)
        val previousCache = if (incremental) cacheFile?.let { ScanCacheStore.load(it) } ?: ScanCache() else ScanCache()
        val index = ScanExtractionIndex(previous = previousCache, incremental = incremental)
        val edges = extractEdges(root, allFiles, projectDirs, index)
        val communities = extractCommunities(repoMap)
        val sections = extractSections(root, allFiles, index)

        val graph = GraphModel(
            schemaVersion = GraphModel.SCHEMA_VERSION,
            nodes = nodes + sections.map { it.node },
            edges = edges
                + sections.map { section ->
                    GraphEdge(source = section.fileRelative, target = section.node.id, type = EdgeType.HAS_SECTION.wire)
                }
                + buildSubsectionEdges(sections),
            communities = communities
        )

        output.parentFile.mkdirs()
        mapper.writerWithDefaultPrettyPrinter().writeValue(output, graph)
        persistCache(index, allFiles, root)
        logger.lifecycle("Graphify scan complete: ${nodes.size} nodes, ${edges.size} edges, ${communities.size} communities -> ${output.absolutePath}")
    }

    private fun persistCache(index: ScanExtractionIndex, allFiles: List<Path>, root: Path) {
        if (!incremental) return
        val cache = cacheFile ?: return
        val livePaths = allFiles.mapNotNull { safe { root.relativize(it).toString() } }.toSet()
        ScanCacheStore.save(cache, index.updatedCache(livePaths))
        val stats = index.stats
        logger.lifecycle(
            "Graphify incremental cache: ${stats.trusted} trusted, ${stats.reusedByHash} reused by hash, ${stats.parsed} parsed -> ${cache.absolutePath}"
        )
    }

    private val excludedDirNames = setOf("build", "node_modules", ".gradle", ".git", ".idea", "target")

    private fun isExcluded(path: Path, root: Path, matchers: List<PathMatcher>, customExcludedNames: Set<String>): Boolean {
        if (path == root) return false
        val relative = safe { root.relativize(path) } ?: return true
        if (matchers.any { it.matches(relative) }) return true
        val name = path.fileName.toString()
        if (excludedDirNames.contains(name) || customExcludedNames.contains(name)) return true
        return false
    }

    private fun extractExcludedNamesFromPatterns(patterns: List<String>): Set<String> {
        return patterns.flatMap { pattern ->
            pattern.split("/", "**").filter { it.isNotEmpty() && it.all { c -> c.isLetterOrDigit() || c == '_' || c == '-' || c == '.' } }
        }.toSet()
    }

    private fun isProjectMarker(path: Path): Boolean {
        val name = path.fileName.toString()
        return name == "build.gradle.kts" || name == "build.gradle" ||
                name == "pom.xml" || name == "package.json"
    }

    private fun extractNodes(
        root: Path,
        fileData: List<FileInfo>,
        dirs: List<Path>,
        projects: List<Path>,
        repoMap: Map<Path, String>
    ): List<GraphNode> {
        val nodes = mutableListOf<GraphNode>()
        val addedDirs = mutableSetOf<String>()

        for (dir in dirs) {
            val relative = safe { root.relativize(dir).toString() } ?: continue
            val isProject = projects.contains(dir)
            nodes.add(
                GraphNode(
                    id = relative,
                    label = dir.fileName.toString(),
                    type = if (isProject) NodeType.PROJECT.wire else NodeType.DIRECTORY.wire,
                    community = repoMap[dir]
                )
            )
            addedDirs.add(relative)
        }

        for ((path, size) in fileData) {
            val relative = safe { root.relativize(path).toString() } ?: continue
            nodes.add(
                GraphNode(
                    id = relative,
                    label = path.fileName.toString(),
                    type = NodeType.FILE.wire,
                    community = repoMap[path.parent],
                    metadata = mapOf(
                        "extension" to (path.extension ?: ""),
                        "size" to size
                    )
                )
            )
        }

        return nodes
    }

    private fun extractEdges(
        root: Path,
        files: List<Path>,
        projectDirs: Set<Path>,
        index: ScanExtractionIndex
    ): List<GraphEdge> {
        val edges = mutableListOf<GraphEdge>()

        for (file in files) {
            val parent = file.parent
            val src = safe { root.relativize(parent).toString() } ?: continue
            val tgt = safe { root.relativize(file).toString() } ?: continue
            edges.add(GraphEdge(source = src, target = tgt, type = EdgeType.CONTAINS.wire))
        }

        val kotlinFiles = files.filter { it.extension == "kt" || it.extension == "kts" }
        for (file in kotlinFiles) {
            val relative = safe { root.relativize(file).toString() } ?: continue
            val imports = index.extraction(file, relative).imports
            for (import in imports) {
                val targetDir = resolveImportToDir(import, projectDirs, root)
                if (targetDir != null) {
                    val target = safe { root.relativize(targetDir).toString() } ?: continue
                    val fileParent = safe { root.relativize(file.parent).toString() } ?: continue
                    if (target != fileParent) {
                        val src = safe { root.relativize(file).toString() } ?: continue
                        edges.add(
                            GraphEdge(source = src, target = target, type = EdgeType.IMPORT.wire, label = import)
                        )
                    }
                }
            }
        }

        val adocFiles = files.filter { it.extension == "adoc" || it.extension == "ad" }
        for (file in adocFiles) {
            val relative = safe { root.relativize(file).toString() } ?: continue
            val extraction = index.extraction(file, relative)
            for (ref in extraction.adocReferences) {
                val targetFile = safe { resolveReferenceToFile(file.parent, ref) }
                if (targetFile != null && safe { Files.exists(targetFile) } == true) {
                    val src = safe { root.relativize(file).toString() } ?: continue
                    val tgt = safe { root.relativize(targetFile).toString() } ?: continue
                    edges.add(GraphEdge(source = src, target = tgt, type = EdgeType.REFERENCE.wire))
                }
            }
            for (ref in extraction.tocReferences) {
                val targetFile = resolveTocReference(file, ref, files)
                if (targetFile != null && safe { Files.exists(targetFile) } == true) {
                    val src = safe { root.relativize(file).toString() } ?: continue
                    val tgt = safe { root.relativize(targetFile).toString() } ?: continue
                    edges.add(GraphEdge(source = src, target = tgt, type = EdgeType.REFERENCE.wire, label = "toc_entry"))
                }
            }
        }

        val idxFiles = files.filter { it.fileName.toString() == "INDEX.adoc" }
        for (file in idxFiles) {
            val relative = safe { root.relativize(file).toString() } ?: continue
            val agentRefs = index.extraction(file, relative).agentReferences
            for (ref in agentRefs) {
                val src = safe { root.relativize(file).toString() } ?: continue
                edges.add(GraphEdge(source = src, target = ref, type = EdgeType.AGENT_REFERENCE.wire))
            }
        }

        return edges
    }

    private fun resolveImportToDir(import: String, projectDirs: Set<Path>, root: Path): Path? {
        for (projDir in projectDirs) {
            val srcDir = projDir.resolve("src/main/kotlin")
            val packageDir = srcDir.resolve(import.replace('.', '/').substringBeforeLast("/"))
            if (safe { Files.isDirectory(packageDir) } == true) return projDir
        }
        return null
    }

    private fun resolveTocReference(fromFile: Path, ref: String, allFiles: List<Path>): Path? {
        val direct = safe { resolveReferenceToFile(fromFile.parent, ref) }
        if (direct != null && safe { Files.isRegularFile(direct) } == true) return direct
        return allFiles.firstOrNull { it.fileName.toString() == ref }
    }

    private fun resolveReferenceToFile(base: Path, ref: String): Path? {
        val candidates = listOf(
            base.resolve(ref).normalize(),
            base.resolve("../$ref").normalize(),
            Path.of(ref)
        )
        return candidates.firstOrNull { safe { Files.isRegularFile(it) } == true }
    }

    private fun buildRepoMap(root: Path, files: List<Path>): Map<Path, String> {
        val result = mutableMapOf<Path, String>()
        for (file in files) {
            var parent: Path? = file.parent
            while (parent != null && parent != root && parent != root.parent) {
                val gitDir = parent.resolve(".git")
                if (safe { Files.isDirectory(gitDir) } == true || safe { Files.isRegularFile(gitDir) } == true) {
                    result[file.parent] = parent.fileName.toString()
                    break
                }
                parent = parent.parent
            }
        }
        return result
    }

    private fun extractCommunities(repoMap: Map<Path, String>): List<GraphCommunity> {
        return repoMap.values
            .groupingBy { it }
            .eachCount()
            .map { (name, size) -> GraphCommunity(id = name, label = name, size = size) }
    }

    private data class FileInfo(val path: Path, val size: Long)

    private data class SectionInfo(val node: GraphNode, val fileRelative: String, val level: Int)

    private fun buildSubsectionEdges(sections: List<SectionInfo>): List<GraphEdge> {
        val edges = mutableListOf<GraphEdge>()
        val stack = mutableListOf<SectionInfo>()
        for (section in sections) {
            while (stack.isNotEmpty() && stack.last().level >= section.level) stack.removeAt(stack.lastIndex)
            stack.lastOrNull()?.let { parent ->
                edges.add(GraphEdge(source = parent.node.id, target = section.node.id, type = EdgeType.SUBSECTION.wire))
            }
            stack.add(section)
        }
        return edges
    }

    private fun extractSections(root: Path, files: List<Path>, index: ScanExtractionIndex): List<SectionInfo> {
        val sections = mutableListOf<SectionInfo>()
        val adocFiles = files.filter { it.extension == "adoc" || it.extension == "ad" }
        val idCounts = mutableMapOf<String, Int>()
        for (file in adocFiles) {
            val fileRelative = safe { root.relativize(file).toString() } ?: continue
            for (section in index.extraction(file, fileRelative).sections) {
                val baseId = "$fileRelative#${section.title}"
                val count = idCounts.merge(baseId, 1, Int::plus) ?: 1
                val sectionId = if (count == 1) baseId else "$baseId~$count"
                sections.add(
                    SectionInfo(
                        node = GraphNode(
                            id = sectionId,
                            label = section.title,
                            type = NodeType.SECTION.wire,
                            metadata = mapOf(
                                "level" to section.level,
                                "line" to section.line,
                                "source" to fileRelative
                            )
                        ),
                        fileRelative = fileRelative,
                        level = section.level
                    )
                )
            }
        }
        return sections
    }

    private fun <T> safe(block: () -> T): T? {
        return try { block() } catch (_: Exception) { null }
    }
}
