package com.dkonopelkin.android.impact.analysis.model

/**
 * @property changedModuleList - имена изменённых проектов.
 * В этом списке проекты для которых непосредственно менялся код. Для них стоит прогонять все проверки
 * @property affectedModuleList - имена проектов, зависимых от измененных.
 * Зависимости которые непосредственно зависят от измененных проектов. Например, для них стоит прогнать unit-тесты
 * @property transitivelyAffectedModuleList - имена проектов, зависимых от измененных транзитивно.
 * Зависимости которые транзитивно зависят от измененных проектов. Пригодятся для функциональных и UI-тестов
 *
 * Множества не пересекающиеся.
 * То есть если какой-то модуль был в [changedModuleList], то его не будет в других сетах.
 * Аналогично если что-то лежит в [affectedModuleList], то его не будет в [transitivelyAffectedModuleList]
 * Даже если кто-то его транзитивно использует.
 */
data class ProjectChanges(
    val changedModuleList: Set<String>,
    val affectedModuleList: Set<String>,
    val transitivelyAffectedModuleList: Set<String>,
)