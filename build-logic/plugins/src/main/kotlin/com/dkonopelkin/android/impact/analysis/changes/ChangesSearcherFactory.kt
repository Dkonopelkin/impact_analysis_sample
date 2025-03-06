package com.dkonopelkin.android.impact.analysis.changes

import org.gradle.api.Project
import com.dkonopelkin.android.impact.analysis.changes.file.ChangedFilesReportManager
import com.dkonopelkin.android.impact.analysis.changes.file.FileChangesSearcher
import com.dkonopelkin.android.impact.analysis.changes.git.GitChangesSearcher
import java.io.File

internal class ChangesSearcherFactory(
    private val reportManager: ChangedFilesReportManager,
) {

    fun create(
        hasGit: Boolean,
        rootProject: Project,
        target: String
    ): ChangesSearcher {
        val changesSearcher = if (hasGit) {
            createGitChangesSearcher(rootProject = rootProject, target = target)
        } else {
            val reportFile = reportManager.getDefaultReportFile(rootProject)
            if (!reportFile.exists()) {
                throw IllegalStateException("Can't find file: ${reportFile.canonicalPath}")
            }
            val createFileChangesSearcher = createFileChangesSearcher(reportFile)
            createFileChangesSearcher
        }
        return changesSearcher
    }

    fun createFileChangesSearcher(
        reportFile: File,
    ): ChangesSearcher {
        return FileChangesSearcher(reportFile, reportManager)
    }

    fun createGitChangesSearcher(
        rootProject: Project,
        target: String
    ): ChangesSearcher {
        return GitChangesSearcher(
            gitRootDir = rootProject.rootDir,
            providers = rootProject.providers,
            target = target,
        )
    }
}