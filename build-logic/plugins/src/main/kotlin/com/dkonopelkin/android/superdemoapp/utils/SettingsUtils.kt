package com.dkonopelkin.android.superdemoapp.utils

import org.gradle.api.Project
import java.io.File

internal class SettingsUtils {

    /**
     * Возвращает список всех модулей проекта в формате [Pair] (имя модуля, путь к модулю).
     */
    fun getAllModules(project: Project): List<Pair<String, String>> {
        val extra = project.extensions.extraProperties
        val rules = extra["rules"] as MutableMap<String, (String) -> List<Pair<String, String>>>
        val allModules = rules.map { (ruleName, rule) ->
            val modulesInRule = extra[ruleName] as MutableList<String>
            modulesInRule.map { path -> rule(path) }.flatten()
        }.flatten()
        val existedModules = allModules.filter { (_, projectPath) ->
            val projectDir = File("${project.rootDir.absolutePath}/$projectPath")
            projectDir.exists()
        }
        return existedModules
    }
}