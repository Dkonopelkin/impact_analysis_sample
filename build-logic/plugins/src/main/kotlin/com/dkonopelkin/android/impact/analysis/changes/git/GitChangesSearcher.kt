package com.dkonopelkin.android.impact.analysis.changes.git

import org.gradle.api.provider.ProviderFactory
import com.dkonopelkin.android.impact.analysis.changes.ChangesSearcher
import com.dkonopelkin.android.impact.analysis.changes.git.GitDiffParser.parseGitDiffLine
import com.dkonopelkin.android.impact.analysis.changes.model.ChangedFile
import java.io.File

/**
 * Класс получает измененные файлы для указанной директории, на основе git diff
 * Полученные изменения кешируются
 */
internal class GitChangesSearcher(
    private val gitRootDir: File,
    private val providerFactory: ProviderFactory,
    private val targetBranch: String
) : ChangesSearcher {

    private val cache: MutableMap<ChangesSearcher.CacheKey, List<ChangedFile>> = mutableMapOf()
    private val gitDiff by lazy { gitDiffWith(targetBranch) }

    init {
        require(gitRootDir.exists()) { "Directory ${gitRootDir.canonicalPath} doesn't exist" }
        require(gitRootDir.canRead()) { "Directory ${gitRootDir.canonicalPath} is not readable" }
    }

    override fun computeChanges(targetDirectory: File, excludedDirectories: Iterable<File>): List<ChangedFile> {
        val result = cache.getOrPut(ChangesSearcher.CacheKey(targetDirectory, excludedDirectories)) {
            computeChangedFilesInDirectory(targetDirectory, excludedDirectories)
        }
        return result
    }

    /**
     * Возвращает git diff между HEAD и [target]
     */
    private fun gitDiffWith(target: String): Set<ChangedFile> {
        val committedChanges = getRawGitDiff(target)
        val workTreeChanges = getRawGitDiff("HEAD")
        val diffResult = committedChanges.plus(workTreeChanges)

        println("git diff result:")
        println(diffResult)
        return diffResult
            .lineSequence()
            .filterNot { it.isBlank() }
            .map { line -> parseGitDiffLine(line).asChangedFile(gitRootDir) }
            .toSet()
    }

    private fun getRawGitDiff(targetBranch: String): String {
        val command = arrayOf("git", "diff", "--name-status", "$targetBranch")
        println(command.joinToString(separator = " "))
        val result = providerFactory.exec { commandLine(*command) }
            .standardOutput
            .asText
            .get()
        return result
    }

    private fun computeChangedFilesInDirectory(
        targetDirectory: File,
        excludedDirectories: Iterable<File> = emptyList()
    ): List<ChangedFile> {
        if (!targetDirectory.toPath().startsWith(gitRootDir.toPath())) {
            throw IllegalArgumentException("$targetDirectory must be inside $gitRootDir")
        }
        val targetPath = targetDirectory.toPath()
        val excludedPaths = excludedDirectories.map { it.toPath() }
        return gitDiff
            .filter { changedFile -> changedFile.file.toPath().startsWith(targetPath) }
            .filterNot { changedFile ->
                excludedPaths.any { changedFile.file.toPath().startsWith(it) }
            }
            .toList()
    }
}