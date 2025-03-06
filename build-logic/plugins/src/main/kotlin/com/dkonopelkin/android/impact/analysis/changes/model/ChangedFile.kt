package com.dkonopelkin.android.impact.analysis.changes.model

import java.io.File

internal data class ChangedFile(
    val rootDir: File,
    val file: File,
    val changeType: ChangeType
) {

    init {
        require(file.startsWith(rootDir)) { "File $file must be within $rootDir" }
    }

    override fun toString(): String {
        return "\n$file"
    }
}
