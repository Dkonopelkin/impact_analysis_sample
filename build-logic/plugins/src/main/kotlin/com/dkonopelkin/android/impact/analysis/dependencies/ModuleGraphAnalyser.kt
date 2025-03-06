package com.dkonopelkin.android.impact.analysis.dependencies

import com.dkonopelkin.android.superdemoapp.dependencies.ModuleInfo

internal class ModuleGraphAnalyser {

    fun findAffectedModules(
        moduleDependantMap: Map<ModuleInfo, Set<ModuleInfo>>,
        modifiedModules: Set<ModuleInfo>
    ): AnalysisResult {
        val affectedModules = mutableSetOf<ModuleInfo>()
        val transitivelyAffectedModules = mutableSetOf<ModuleInfo>()

        fun dfs(module: ModuleInfo, visited: MutableSet<ModuleInfo>) {
            if (module in visited) return
            visited.add(module)

            moduleDependantMap[module]?.forEach { dependentModule ->
                if (dependentModule !in modifiedModules) {
                    transitivelyAffectedModules.add(dependentModule)
                    dfs(dependentModule, visited)
                }
            }
        }

        modifiedModules.forEach { changedModule ->
            moduleDependantMap[changedModule]?.forEach { dependentModule ->
                if (dependentModule !in modifiedModules) {
                    affectedModules.add(dependentModule)
                    dfs(dependentModule, mutableSetOf(changedModule))
                }
            }
        }

        /**
         * Сделаем множества не пересекающимися
         * */
        affectedModules -= modifiedModules
        transitivelyAffectedModules -= affectedModules

        return AnalysisResult(affectedModules, transitivelyAffectedModules)
    }

    data class AnalysisResult(
        val affectedModuleList: Set<ModuleInfo>,
        val transitivelyAffectedModuleList: Set<ModuleInfo>
    )
}