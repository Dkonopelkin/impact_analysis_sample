package com.dkonopelkin.android.impact.analysis.changes.model

/**
 * @param code code of --diff-filter in git diff
 */
internal enum class ChangeType(val code: Char) {

    ADDED('A'),
    COPIED('C'),
    DELETED('D'),
    MODIFIED('M'),
    RENAMED('R');

    internal companion object {

        fun getTypeByCode(code: Char): ChangeType {
            return values().firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Cannot parse diff type with code $code")
        }
    }
}
