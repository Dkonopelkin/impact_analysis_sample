package com.dkonopelkin.android.impact.analysis.changes.git.model

import com.dkonopelkin.android.impact.analysis.changes.model.ChangeType
import com.dkonopelkin.android.impact.analysis.changes.model.ChangedFile
import java.io.File

internal data class GitDiffLine(
    val path: String,
    val changeType: ChangeType
) {

    fun asChangedFile(rootDir: File): ChangedFile {
        return ChangedFile(rootDir, File(rootDir, path), changeType)
    }
}