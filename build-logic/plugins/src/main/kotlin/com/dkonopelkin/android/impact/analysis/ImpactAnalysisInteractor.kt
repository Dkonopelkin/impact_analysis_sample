package com.dkonopelkin.android.impact.analysis

import com.dkonopelkin.android.base.GitUtils.hasGit
import org.gradle.api.Project
import com.dkonopelkin.android.impact.analysis.ImpactAnalysisConst.ENABLE_DEBUG_LOG
import com.dkonopelkin.android.impact.analysis.ImpactAnalysisConst.TARGET_BRANCH
import com.dkonopelkin.android.impact.analysis.changes.ChangesSearcherFactory
import com.dkonopelkin.android.impact.analysis.dependencies.ModuleGraphAnalyser
import com.dkonopelkin.android.impact.analysis.dependencies.ModuleGraphBuilder
import com.dkonopelkin.android.impact.analysis.model.ProjectChanges
import com.dkonopelkin.android.superdemoapp.dependencies.ModuleInfo
import com.dkonopelkin.android.superdemoapp.dependencies.ModuleInfoMapper
import com.dkonopelkin.android.superdemoapp.utils.SettingsUtils
import java.io.File

/**
 * С помощью решения из SuperDemoApp собираются данные всех модулей, подключенных к root.
 * Подобное решение работает значительно быстрее, чем сбор данных о модулях с помощью Gradle.
 * Далее собирается информация о том, какие модули были изменены.
 * При наличии git используется напрямую он (без записи файла с изменениями), иначе используется файл с предварительно
 * закешированными изменениями [ImpactAnalysisChangedFileReportTask]
 * После формируется карта консьюмеров модулей и рекурсивно вычисляется какие модули были затронуты изменениями.
 */
@Suppress("LongParameterList")
internal class ImpactAnalysisInteractor(
    private val moduleGraphBuilder: ModuleGraphBuilder,
    private val moduleGraphAnalyser: ModuleGraphAnalyser,
    private val moduleInfoMapper: ModuleInfoMapper,
    private val settingsUtils: SettingsUtils,
    private val changesSearcherFactory: ChangesSearcherFactory,
) {

    fun findChanges(rootProject: Project, excludeAffectedModules: Set<String>): ProjectChanges {
        val startTime = System.currentTimeMillis()

        val allModulesInfoList = getModuleInfoList(rootProject)

        /**
         * В зависимости от наличия .git репозитория выбирается стратегия откуда брать результаты impact-анализа.
         * Если git отсутствует, то информация об изменениях будет взята из созданного ранее xml-отчета.
         */
        val changesSearcher = changesSearcherFactory.create(
            hasGit = hasGit(rootProject),
            rootProject = rootProject,
            target = TARGET_BRANCH
        )

        /**
         * Сет модулей, код которых изменен
         * */
        val modifiedModules = allModulesInfoList
            .associateWith { changesSearcher.computeChanges(File(it.absolutePath)) }
            .filterValues { it.isNotEmpty() }

        /**
         * Получаем карту модулей, где
         * @key - информация о модуле
         * @value - список модулей, которые используют в своём build.gradle.kts модуль ключа (зависят от него).
         * Исключаем модули, которые не должны учитываться как affected. Например sample-приложения
         */
        val moduleDependantMap: Map<ModuleInfo, Set<ModuleInfo>> =
            moduleGraphBuilder.buildModuleDependantGraph(allModulesInfoList)
                .mapValues { (_, dependents) ->
                    dependents.filterNot { it.name in excludeAffectedModules }.toSet()
                }

        /**
         * Находим модули которые зависят от измененных модулей напрямую и транзитивно
         */
        val result = moduleGraphAnalyser.findAffectedModules(
            moduleDependantMap = moduleDependantMap,
            modifiedModules = modifiedModules.keys
        )

        if (ENABLE_DEBUG_LOG) {
            println("$LOG_TAG Found changes in ${System.currentTimeMillis() - startTime} ms")
        }
        return ProjectChanges(
            changedModuleList = modifiedModules.keys.map { it.name }.toSet(),
            affectedModuleList = result.affectedModuleList.map { it.name }.toSet(),
            transitivelyAffectedModuleList = result.transitivelyAffectedModuleList.map { it.name }.toSet()
        )
    }

    private fun getModuleInfoList(rootProject: Project): List<ModuleInfo> {
        val allModules = settingsUtils.getAllModules(rootProject)
        val rootDirPath = rootProject.rootProject.rootDir.absolutePath
        val modulesInfo = moduleInfoMapper.mapToModuleInfo(allModules, rootDirPath)
        return modulesInfo
    }

    companion object {

        const val LOG_TAG = "ImpactAnalysisInteractor"
    }
}