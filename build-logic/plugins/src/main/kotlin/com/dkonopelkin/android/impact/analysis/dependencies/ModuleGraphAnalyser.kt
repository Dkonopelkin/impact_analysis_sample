package com.dkonopelkin.android.impact.analysis.dependencies

import com.dkonopelkin.android.superdemoapp.dependencies.ModuleInfo

internal class ModuleGraphAnalyser {

    fun findAffectedModules(
        moduleDependantMap: Map<ModuleInfo, Set<ModuleInfo>>,
        changedModules: Set<ModuleInfo>
    ): AnalysisResult {
        val affectedModules = mutableSetOf<ModuleInfo>()
        val transitivelyAffectedModules = mutableSetOf<ModuleInfo>()

        fun dfs(module: ModuleInfo, visited: MutableSet<ModuleInfo>) {
            if (module in visited) return
            visited.add(module)

            moduleDependantMap[module]?.forEach { dependentModule ->
                if (dependentModule !in changedModules) {
                    transitivelyAffectedModules.add(dependentModule)
                    dfs(dependentModule, visited)
                }
            }
        }

        changedModules.forEach { changedModule ->
            moduleDependantMap[changedModule]?.forEach { dependentModule ->
                if (dependentModule !in changedModules) {
                    affectedModules.add(dependentModule)
                    dfs(dependentModule, mutableSetOf(changedModule))
                }
            }
        }

        /**
         * Сделаем множества не пересекающимися
         * */
        affectedModules -= changedModules
        transitivelyAffectedModules -= (affectedModules + changedModules)

        return AnalysisResult(affectedModules, transitivelyAffectedModules)
    }

    data class AnalysisResult(
        val affectedModuleList: Set<ModuleInfo>,
        val transitivelyAffectedModuleList: Set<ModuleInfo>
    )
}