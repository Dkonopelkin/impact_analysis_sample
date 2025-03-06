package com.dkonopelkin.android.impact.analysis.changes

import com.dkonopelkin.android.impact.analysis.changes.model.ChangedFile
import java.io.File

internal interface ChangesSearcher {

    /**
     * Общий интерфейс для получения измененных файлов из разных источников (git/report)
     */
    fun computeChanges(
        targetDirectory: File,
        excludedDirectories: Iterable<File> = emptyList()
    ): List<ChangedFile>

    data class CacheKey(val targetDirectory: File, val excludedDirectories: Iterable<File>)
}