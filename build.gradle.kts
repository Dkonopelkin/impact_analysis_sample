import dev.iurysouza.modulegraph.Theme

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    apply(from = "${rootDir}/build_scripts/module_rules.gradle.kts")
    apply(from = "${rootDir}/build_scripts/settings/app.gradle.kts")
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.dkonopelkin.impact-analysis")
    id("dev.iurysouza.modulegraph") version "0.12.0"
}

// Для простоты создадим stub-таски, которые при запуске выведут сообщение
// Вместо подключения реального detekt плагина.
subprojects {
    project.tasks.create("detekt") {
        doLast {
            println("detekt stub called for module ${project.name}")
        }
    }
    tasks.withType<Test>().all {
        // eager creation for test tasks
    }
}

moduleGraphConfig {
    readmePath.set("${rootDir}/README.md")
    heading.set("### Module Graph")
    theme.set(
        Theme.BASE(
            mapOf(
                "primaryTextColor" to "#fff",
                "primaryColor" to "#5a4f7c",
                "primaryBorderColor" to "#5a4f7c",
                "lineColor" to "#f5a623",
                "tertiaryColor" to "#40375c",
                "fontSize" to "12px",
            ),
            focusColor = "#FA8140"
        ),
    )
}