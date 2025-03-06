package com.dkonopelkin.android.impact.analysis.changes.file

import com.dkonopelkin.android.impact.analysis.changes.ChangesSearcher
import com.dkonopelkin.android.impact.analysis.changes.model.ChangedFile
import java.io.File

/**
 * Класс получает измененные файлы для указанной директории, на основе файла отчёта, созданного ранее
 * Полученные изменения кешируются
 */
internal class FileChangesSearcher(
    private val reportFile: File,
    private val changedFilesReportManager: ChangedFilesReportManager,
) : ChangesSearcher {

    private val changes by lazy { changedFilesReportManager.readChangedFilesFromReport(reportFile) }
    private val cache: MutableMap<ChangesSearcher.CacheKey, List<ChangedFile>> = mutableMapOf()

    override fun computeChanges(targetDirectory: File, excludedDirectories: Iterable<File>): List<ChangedFile> {
        return cache.getOrPut(ChangesSearcher.CacheKey(targetDirectory, excludedDirectories)) {
            parseFile(targetDirectory, excludedDirectories)
        }
    }

    private fun parseFile(targetDirectory: File, excludedDirectories: Iterable<File>): List<ChangedFile> {
        val entry = changes.entries.firstOrNull { (moduleInfo, _) ->
            targetDirectory.absolutePath.endsWith(moduleInfo.relativePath)
        }
        return entry?.value?.filterNot { file -> isExcluded(file, excludedDirectories) } ?: emptyList()
    }

    private fun isExcluded(file: ChangedFile, excludedDirectories: Iterable<File>): Boolean {
        return excludedDirectories.any { excludedDir ->
            file.file.absolutePath.startsWith(excludedDir.absolutePath)
        }
    }
}