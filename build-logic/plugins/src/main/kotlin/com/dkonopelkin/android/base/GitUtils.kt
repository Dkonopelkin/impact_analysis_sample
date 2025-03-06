package com.dkonopelkin.android.base

import org.gradle.api.Project
import java.io.File

internal object GitUtils {

    fun hasGit(rootProject: Project): Boolean {
        var folder: File? = rootProject.projectDir
        while (folder != null) {
            if (File(folder, ".git").exists()) {
                return true
            }
            folder = folder.parentFile
        }
        return false
    }
}