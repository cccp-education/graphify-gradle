package com.cheroliv.graphify

import org.gradle.api.Plugin
import org.gradle.api.Project

class GraphifyPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("graphify", GraphifyExtension::class.java)

        val collectFromWorkspace = project.tasks.register("collectFromWorkspace", ScanWorkspaceTask::class.java) { task ->
            task.group = "collect"
            task.rootDir = extension.rootDir.get()
            task.outputFile = extension.outputFile.get()
            task.excludePatterns = extension.excludePatterns.get()
            task.doNotTrackState("Full filesystem scan — inherently non-incremental")
        }

        project.tasks.register("verifyDagAcyclic", VerifyDagAcyclicTask::class.java) { task ->
            task.group = "verify"
            task.dagLevels = extension.dagLevels.get()
            task.foundryDir = extension.foundryDir.get().absolutePath
        }

        project.tasks.register("collectAndVerify") { task ->
            task.group = "collect"
            task.description = "Chain collectFromWorkspace + verifyDagAcyclic for integrated validation"
            task.dependsOn(collectFromWorkspace)
            task.finalizedBy("verifyDagAcyclic")
        }
    }
}
