package com.dkonopelkin.android.superdemoapp.dependencies

internal class ModuleInfoMapper {

    fun mapToModuleInfo(
        moduleNameToPathList: List<Pair<String, String>>,
        rootDirPath: String
    ): List<ModuleInfo> {
        return moduleNameToPathList
            .map { (projectName, projectPath) -> createModuleInfo(projectName, projectPath, rootDirPath) }
    }

    private fun createModuleInfo(
        projectName: String,
        projectPath: String,
        rootDirPath: String
    ): ModuleInfo {
        return ModuleInfo(
            name = projectName.removePrefix(":"),
            relativePath = "/$projectPath",
            absolutePath = "${rootDirPath}/$projectPath"
        )
    }
}