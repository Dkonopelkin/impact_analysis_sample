package com.dkonopelkin.android.impact.analysis.changes.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import com.dkonopelkin.android.impact.analysis.ImpactAnalysisConst.ENABLE_DEBUG_LOG
import com.dkonopelkin.android.impact.analysis.ImpactAnalysisConst.IMPACT_ANALYSIS_TASK_GROUP
import com.dkonopelkin.android.impact.analysis.ImpactAnalysisConst.TARGET_BRANCH
import com.dkonopelkin.android.impact.analysis.changes.ChangesSearcher
import com.dkonopelkin.android.impact.analysis.changes.ChangesSearcherFactory
import com.dkonopelkin.android.impact.analysis.changes.file.ChangedFilesReportManager
import com.dkonopelkin.android.impact.analysis.changes.model.ChangedFile
import com.dkonopelkin.android.impact.analysis.changes.model.ModuleData
import java.io.File
import javax.inject.Inject

/**
 * Таск сохраняет результат какие файлы были изменены в XML файл, не анализируя зависимости между модулями
 *
 * Требует наличия git и поэтому не может быть исполнен на mirakle.
 * Его задача сделать, чтобы следующие шаги impact анализа работали без git
 * */
internal abstract class ImpactAnalysisChangedFileReportTask
@Inject constructor() : DefaultTask() {

    private val impactFile: File
    private val changedFilesMap: Map<ModuleData, File>
    private val gitChangesSearcher: ChangesSearcher
    private val fileChangesSearcher: ChangesSearcher
    private val changedFilesReportManager = ChangedFilesReportManager()
    private val changesSearcherFactory = ChangesSearcherFactory(changedFilesReportManager)

    init {
        group = IMPACT_ANALYSIS_TASK_GROUP
        description = "Save changed file properties to XML file"

        impactFile = changedFilesReportManager.getDefaultReportFile(project.rootProject)
        changedFilesMap = project.rootProject.subprojects.associate {
            val moduleData = ModuleData(
                name = it.name,
                relativePath = it.projectDir.absolutePath.replace(it.rootProject.projectDir.absolutePath, "")
            )
            moduleData to it.projectDir
        }

        gitChangesSearcher = changesSearcherFactory.createGitChangesSearcher(
            rootProject = project.rootProject,
            target = TARGET_BRANCH,
        )
        fileChangesSearcher = changesSearcherFactory.createFileChangesSearcher(
            reportFile = impactFile
        )
    }

    @TaskAction
    fun action() {
        /**
         * Получаем результаты из git и сохраняем их в файл
         */
        val changesFromGit = getChanges(gitChangesSearcher)
        changedFilesReportManager.writeChangedFilesToReport(impactFile, changesFromGit)

        if (ENABLE_DEBUG_LOG) {
            /**
             * Для упрощения отладки
             * Выводим в лог изменения полученные из git и считанные файла. Списки должны быть одинаковыми
             */
            printChangesFromGit(changesFromGit)
            printChangesFromReport()
        }
        println("Impact analysis: Changes saved to ${impactFile.absolutePath}")
    }

    private fun printChangesFromReport() {
        val changesFromFile = getChanges(fileChangesSearcher)
        println("==========CHANGED_FILES_FROM_FILE==========")
        changesFromFile.forEach { (moduleInfo, changedFileList) ->
            println("$moduleInfo:\n   ${changedFileList.map { it.file }.joinToString("\n   ")}")
        }
        println("==========CHANGED_FILES_FROM_FILE_END==========")
    }

    private fun printChangesFromGit(changesFromGit: Map<ModuleData, List<ChangedFile>>) {
        println("==========CHANGED_FILES_FROM_GIT==========")
        changesFromGit.forEach { (moduleInfo, changedFileList) ->
            println("$moduleInfo:\n   ${changedFileList.map { it.file }.joinToString("\n   ")}")
        }
        println("==========CHANGED_FILES_FROM_GIT_END==========")
    }

    private fun getChanges(
        changesSearcher: ChangesSearcher
    ): Map<ModuleData, List<ChangedFile>> {
        return changedFilesMap
            .mapValues { changesSearcher.computeChanges(it.value) }
            .filterValues { it.isNotEmpty() }
    }

    companion object {

        const val NAME = "impactAnalysisChangedFileReport"
    }
}