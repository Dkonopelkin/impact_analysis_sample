package com.dkonopelkin.android.impact.analysis.changes.git

import com.dkonopelkin.android.impact.analysis.changes.git.model.GitDiffLine
import com.dkonopelkin.android.impact.analysis.changes.model.ChangeType

/**
 *  Класс для парсинга результатов команды git diff. Например:
 *  ```
 *  A README.md
 *  R95 old_name.kt new_name.kt
 *  ```
 */
internal object GitDiffParser {

    internal fun parseGitDiffLine(line: String): GitDiffLine {
        val parts = line.split(Regex("[ \t]"))
            .map { it.trim() }
        if (parts.size < 2) {
            throw IllegalArgumentException("Line has invalid syntax: $line ")
        }
        val diffTypeCode = extractDiffTypeCode(parts)
        val changeType = ChangeType.getTypeByCode(diffTypeCode)
        return if (isSyntaxCorrect(parts, changeType)) {
            GitDiffLine(extractFilePath(parts, changeType), changeType)
        } else {
            throw IllegalArgumentException("Line has invalid syntax: $line ")
        }
    }

    // diff type 'R' and 'C' is followed by probability score e.g. R95, C100 etc
    private fun extractDiffTypeCode(parts: List<String>): Char {
        return parts[0].first()
    }

    @Suppress("MagicNumber")
    private fun isSyntaxCorrect(parts: List<String>, changeType: ChangeType): Boolean {
        return when (changeType) {
            ChangeType.ADDED, ChangeType.MODIFIED, ChangeType.DELETED -> parts.size == 2
            ChangeType.COPIED, ChangeType.RENAMED -> parts.size == 3
        }
    }

    private fun extractFilePath(parts: List<String>, changeType: ChangeType): String {
        return when (changeType) {
            ChangeType.ADDED, ChangeType.MODIFIED, ChangeType.DELETED -> parts[1]
            ChangeType.COPIED, ChangeType.RENAMED -> parts[2]
        }
    }
}
