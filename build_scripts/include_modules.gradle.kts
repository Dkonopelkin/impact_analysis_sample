import java.io.File

val rules: MutableMap<String, (String) -> List<Pair<String, String>>> by extra

val allModules = rules.map { (ruleName, rule) ->
    val modulesInRule = extra[ruleName] as MutableList<String>
    modulesInRule.map { path -> rule(path) }.flatten()
}.flatten()

allModules.forEach { (moduleName, moduleDir) ->
    val moduleDirFile = File(rootDir, moduleDir)
    if (moduleDirFile.exists()) {
        include(moduleName)
        project(moduleName).projectDir = moduleDirFile
    }
}