package com.dkonopelkin.android.impact.analysis

import com.dkonopelkin.android.impact.analysis.ImpactAnalysisConst.ENABLE_DEBUG_LOG
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import com.dkonopelkin.android.impact.analysis.ImpactAnalysisConst.IMPACT_ANALYSIS
import com.dkonopelkin.android.impact.analysis.changes.ChangesSearcherFactory
import com.dkonopelkin.android.impact.analysis.changes.file.ChangedFilesReportManager
import com.dkonopelkin.android.impact.analysis.changes.tasks.ImpactAnalysisChangedFileReportTask
import com.dkonopelkin.android.impact.analysis.dependencies.ModuleGraphAnalyser
import com.dkonopelkin.android.impact.analysis.dependencies.ModuleGraphBuilder
import com.dkonopelkin.android.impact.analysis.model.ProjectChanges
import com.dkonopelkin.android.superdemoapp.dependencies.ModuleInfoMapper
import com.dkonopelkin.android.superdemoapp.utils.SettingsUtils

/**
 * Плагин позволяет анализировать изменения в проекте и запускать таски только для изменённых (Modified) модулей.
 *
 * Android-сообщество привыкло использовать термин "модуль", но в gradle нет такого понятия. Определим терминологию:
 * - Проект (Project), это всё приложение (rootProject в терминологии Gradle)
 * - Модуль (Module), это подпроект (subproject в терминологии Gradle)
 *
 * Изменный (Modified) модуль, это общее название для модулей, в которых были изменения. Modified модули делятся на:
 * - Непосредственно измененные (Changed), если в модули были изменены файлы.
 * - Затронутые (Affected), если если использует Changed модуль напрямую.
 * - Транзитивно затронутые (Transitively Affected), если использует Affected или другой Transitively Affected модуль
 * // FIXME: @dkonopelkin вставить ссылку на пример

 * Файлы могут быть только Changed.
 *
 * В зависимости от стратегии оптимизации, таски будут запущены для разных групп модулей:
 * @property [OptimizeStrategy.ONLY_CHANGED_MODULES]
 * Таски, будут запущены только для changed модулей.
 *
 * @property [OptimizeStrategy.CHANGE_MODULES_WITH_DIRECT_DEPENDENCIES]
 * Таски, будут запущены для changes + affected модулей.
 *
 * @property [OptimizeStrategy.CHANGE_MODULES_WITH_TRANSITIVE_DEPENDENCIES]
 * Таски, будут запущены для changes + affected + transitively affected модулей.
 *
 * @property tasksToProxyInSubProjects Таски, которые нужно создать в подпроектах. Обертки в подпроектах нужны для
 *      того, чтобы избежать ситуации, в которой мы вызываем таску модуля, а там нет её нет. Связь optional
 *      таски (н-р optionalDetekt) подпроекта и реальной таски (н-р detekt) настраивается в момент добавления тасок
 *      подпроекта в случае наличия реальной таски. Если реальной таски нет, то optional таска вызовется, но
 *      ничего делать не будет.
 * @property modulesToExcludeFromAffectedLocally Модули, которые нужно исключить при локальной проверке (!isCiBuild)
 *      для ускорения работы optimized тасок, так как любое изменение транзитивно будет задевать application модули,
 *      а это влечёт за собой конфигурацию и компиляцию всего проекта. Следует исключать эти модули, только если
 *      они попали в список транзитивно затронутых модулей. Если пользователь изменил код самого модуля, то
 *      он не будет исключен.
 *
 * Чтобы увидеть список доступных тасок - вызовите `./gradlew :tasks --group="Impact Analysis"`
 */
internal class ImpactAnalysisPlugin : Plugin<Project> {

    private val isCiBuild = false
    private val moduleGraphBuilder by lazy { ModuleGraphBuilder() }
    private val moduleGraphAnalyser by lazy { ModuleGraphAnalyser() }
    private val moduleInfoMapper by lazy { ModuleInfoMapper() }
    private val settingsUtils by lazy { SettingsUtils() }
    private val changedFilesReportManager by lazy { ChangedFilesReportManager() }
    private val changesSearcherFactory by lazy { ChangesSearcherFactory(changedFilesReportManager) }
    private val impactAnalysisInteractor by lazy {
        ImpactAnalysisInteractor(
            moduleGraphBuilder = moduleGraphBuilder,
            moduleGraphAnalyser = moduleGraphAnalyser,
            moduleInfoMapper = moduleInfoMapper,
            settingsUtils = settingsUtils,
            changesSearcherFactory = changesSearcherFactory,
        )
    }
    private var cachedChanges: ProjectChanges? = null

    enum class OptimizeStrategy(
        val taskList: Set<String>,
        val targetProjects: (ProjectChanges) -> Set<String>,
    ) {

        ONLY_CHANGED_MODULES(
            taskList = setOf(
                "detekt",
            ),
            targetProjects = { it.changedModuleList }
        ),
        CHANGE_MODULES_WITH_DIRECT_DEPENDENCIES(
            /*
            В реальном проекте тестам достаточно быть выполненными только для changed и affected модулей.
            Но в качестве демонстрации, покажем что таски запускаются и для транзитивных модулей также.*/
            taskList = emptySet(),
            /*taskList = setOf(
                "testDebugUnitTest"
            ),*/
            targetProjects = {
                it.changedModuleList
                    .plus(it.affectedModuleList)
            }
        ),
        CHANGE_MODULES_WITH_TRANSITIVE_DEPENDENCIES(
            /*
            В реальном проекте тестам достаточно быть выполненными только для changed и affected модулей.
            Но в качестве демонстрации, покажем что таски запускаются и для транзитивных модулей также.
            */
            taskList = setOf(
                "testDebugUnitTest"
            ),
            /*taskList = emptySet(),*/
            targetProjects = {
                it.changedModuleList
                    .plus(it.affectedModuleList)
                    .plus(it.transitivelyAffectedModuleList)
            }
        )
    }

    private val tasksToProxyInSubProjects = OptimizeStrategy.ONLY_CHANGED_MODULES.taskList
        .plus(OptimizeStrategy.CHANGE_MODULES_WITH_DIRECT_DEPENDENCIES.taskList)
        .plus(OptimizeStrategy.CHANGE_MODULES_WITH_TRANSITIVE_DEPENDENCIES.taskList)
    private val modulesToExcludeFromAffectedLocally = setOf(
        "app",
    )

    override fun apply(root: Project) {
        if (root.rootProject != root) {
            throw IllegalStateException("Impact analysis plugin should be applied only to root project")
        }

        OptimizeStrategy.values().forEach { strategy ->
            registerOptimizedTasks(
                root = root,
                strategy = strategy,
            )
        }

        registerReportTasks(root)

        root.subprojects {
            for (taskName in tasksToProxyInSubProjects) {
                val optionalTaskProvider = tasks.register(getOptionalTaskName(taskName)) {
                    group = IMPACT_ANALYSIS
                    description = "Вызывает таску '$taskName', если она есть в модуле"
                }
                tasks.named { it == taskName }.configureEach {
                    if (ENABLE_DEBUG_LOG) {
                        println("register :${project.name}:${optionalTaskProvider.name} dependsOn(:${project.name}:$name)")
                    }
                    optionalTaskProvider.get().dependsOn(this)
                }
            }
        }
    }

    private fun registerOptimizedTasks(
        root: Project,
        strategy: OptimizeStrategy,
    ) {
        for (taskName in strategy.taskList) {
            val optimizedTaskName = getOptimizedTaskName(taskName)
            root.tasks.register(optimizedTaskName) {
                group = IMPACT_ANALYSIS
                description = "Вызывает таску '$taskName' только для изменённых модулей"
                mustRunAfter(ImpactAnalysisChangedFileReportTask.NAME)

                val changes = getChanges(root)
                for (changedModule in strategy.targetProjects(changes)) {
                    val optionalTaskName = getOptionalTaskName(taskName)
                    if (ENABLE_DEBUG_LOG) {
                        println("Set :$optimizedTaskName dependsOn(:$changedModule:$optionalTaskName)")
                    }
                    dependsOn(":$changedModule:$optionalTaskName")
                }
            }
        }
    }

    /**
     * task -> optimizedTask
     */
    private fun getOptimizedTaskName(taskName: String): String {
        return "optimized${taskName.replaceFirstChar { it.uppercase() }}"
    }

    /**
     * task -> optionalTask
     */
    private fun getOptionalTaskName(taskName: String): String {
        return "optional${taskName.replaceFirstChar { it.uppercase() }}"
    }

    private fun registerReportTasks(target: Project) {
        target.tasks.register<ImpactAnalysisChangedFileReportTask>(ImpactAnalysisChangedFileReportTask.NAME) {
            group = IMPACT_ANALYSIS
            description = "Генерирует файл со списком измененных файлов в XML, не анализируя зависимости между модулями"
        }
    }

    /**
     * Вызывать только из блока конфигурации таски, чтобы выполнять расчёт лишь в том случае, если какая-то из
     * optimized тасок попала в граф выполнения, а не при каждом apply плагина.
     */
    private fun getChanges(target: Project): ProjectChanges {
        if (cachedChanges == null) {
            cachedChanges = impactAnalysisInteractor.findChanges(
                rootProject = target,
                excludeAffectedModules = if (isCiBuild) {
                    emptySet()
                } else {
                    modulesToExcludeFromAffectedLocally
                }
            )
        }
        val result = requireNotNull(cachedChanges)

        println(
            """Impact Analysis.
                |Changed %s modules: %s.
                |Affected %s modules: %s.
                |Transitively Affected %s modules: %s""".trimMargin().format(
                result.changedModuleList.size.toString(),
                result.changedModuleList.toString(),
                result.affectedModuleList.size.toString(),
                result.affectedModuleList.toString(),
                result.transitivelyAffectedModuleList.size.toString(),
                result.transitivelyAffectedModuleList.toString(),
            )
        )

        return result
    }
}