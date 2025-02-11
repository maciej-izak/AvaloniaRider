package me.fornever.avaloniarider.rider

import com.google.common.collect.Queues
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.application
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseUntil
import com.jetbrains.rider.model.RdProjectDescriptor
import com.jetbrains.rider.model.riderSolutionLifecycle
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.ProjectModelEntity
import com.jetbrains.rider.projectView.workspace.containingProjectEntity
import com.jetbrains.rider.projectView.workspace.getProjectModelEntities
import kotlinx.coroutines.CompletableDeferred

val ProjectModelEntity.projectRelativeVirtualPath: String
    get() {
        val names = Queues.newArrayDeque<String>()
        var current: ProjectModelEntity? = this
        while (current != null && current.descriptor !is RdProjectDescriptor) {
            names.push(current.name)
            current = current.parentEntity
        }
        return names.joinToString("/")
    }

@Suppress("UnstableApiUsage")
suspend fun VirtualFile.getProjectContainingFile(lifetime: Lifetime, project: Project): ProjectModelEntity {
    val logger = Logger.getInstance("me.fornever.avaloniarider.rider.ProjectModelNodeExKt")
    val workspaceModel = WorkspaceModel.getInstance(project)

    application.assertIsDispatchThread()

    val result = CompletableDeferred<ProjectModelEntity>()

    project.solution.riderSolutionLifecycle.isProjectModelReady.adviseUntil(lifetime) { isReady ->
        if (!isReady) return@adviseUntil false
        try {
            logger.debug { "Project model view synchronized" }
            val projectModelEntities = workspaceModel.getProjectModelEntities(this, project)
            logger.debug {
                "Project model nodes for file $this: " + projectModelEntities.joinToString(", ")
            }
            val containingProject = projectModelEntities.asSequence()
                .map { it.containingProjectEntity() }
                .filterNotNull()
                .first()
            result.complete(containingProject)
        } catch (t: Throwable) {
            result.completeExceptionally(t)
        }

        return@adviseUntil true
    }

    return result.await()
}
