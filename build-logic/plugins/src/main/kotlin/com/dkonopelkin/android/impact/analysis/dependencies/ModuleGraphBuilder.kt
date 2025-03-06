package com.dkonopelkin.android.impact.analysis.dependencies

import com.dkonopelkin.android.superdemoapp.dependencies.ModuleInfo
import java.io.File

/**
 * Анализирует зависимости модулей и формирует карту модулей, где
 * @key - информация о модуле
 * @value - список модулей, которые используют в своём build.gradle.kts модуль ключа (зависят от него).
 *
 * В этом смысле Dependant - потребитель. Dependant это антоним к Dependency.
 * То есть, если модуль A зависит от модуля B, то B - dependant A.
 */
internal class ModuleGraphBuilder {

    private val dependencyRegex = """\b[a-zA-Z]+\b\(project\(".*"\)\)""".toRegex()
    private val testDependencyRegex = """([a-z]T|\bt)est([A-Z][a-z]*|)\(""".toRegex()
    private val apiDependencyRegex = """([a-z]A|\ba)pi\(""".toRegex()
    private val projectNameRegex = """".*"""".toRegex()

    fun buildModuleDependantGraph(modulesInfo: List<ModuleInfo>): Map<ModuleInfo, Set<ModuleInfo>> {
        val result = mutableMapOf<ModuleInfo, MutableSet<ModuleInfo>>()
        val modulesInfoMap = modulesInfo.associateBy { it.name }
        val allApiDependenciesMap = mutableMapOf<String, Set<ModuleInfo>>()

        for (moduleInfo in modulesInfo) {
            val deps = collectModuleDependencies(
                targetModule = moduleInfo,
                modulesInfoMap = modulesInfoMap,
                allApiDependenciesMap = allApiDependenciesMap,
                dependencyType = DependencyType.ALL,
            )

            for (dep in deps) {
                if (!result.containsKey(dep)) {
                    result[dep] = mutableSetOf()
                }
                result.getValue(dep) += moduleInfo
            }
        }

        return result
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private fun collectModuleDependencies(
        targetModule: ModuleInfo,
        modulesInfoMap: Map<String, ModuleInfo>,
        allApiDependenciesMap: MutableMap<String, Set<ModuleInfo>>,
        dependencyType: DependencyType,
    ): Set<ModuleInfo> {
        val result = mutableSetOf<ModuleInfo>()

        val fileLines = findBuildGradle(targetModule)?.readLines().orEmpty()
        for (line in fileLines) {
            val dependencyDeclaration = dependencyRegex.find(line)?.value ?: continue
            if ("$" in dependencyDeclaration) continue
            val projectName = getProjectName(dependencyDeclaration)
            val depModule = modulesInfoMap.getValue(projectName)
            when {
                testDependencyRegex.find(dependencyDeclaration) != null -> {
                    result += depModule
                    if (apiDependencyRegex.find(dependencyDeclaration) != null) {
                        result += allApiDependenciesMap.getOrPut(depModule.name) {
                            collectModuleDependencies(
                                targetModule = depModule,
                                modulesInfoMap = modulesInfoMap,
                                allApiDependenciesMap = allApiDependenciesMap,
                                dependencyType = DependencyType.TEST,
                            )
                        }
                    }
                }

                apiDependencyRegex.find(dependencyDeclaration) != null && dependencyType != DependencyType.TEST -> {
                    result += depModule
                    result += allApiDependenciesMap.getOrPut(depModule.name) {
                        collectModuleDependencies(depModule, modulesInfoMap, allApiDependenciesMap, dependencyType)
                    }
                }

                dependencyType != DependencyType.TEST -> result += depModule
            }
        }

        return result
    }

    private fun getProjectName(dependencyDeclaration: String): String {
        return projectNameRegex.find(dependencyDeclaration)!!.value.drop(2).dropLast(1)
    }

    private fun findBuildGradle(moduleInfo: ModuleInfo): File? {
        return File(moduleInfo.absolutePath, "build.gradle.kts").takeIf { it.exists() }
    }

    private enum class DependencyType {
        ALL,
        TEST,
    }
}