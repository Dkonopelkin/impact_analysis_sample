val rules by extra(mutableMapOf<String, (String) -> List<Pair<String, String>>>())

rules["single"] = { path -> listOf(":${path.split("/").last()}" to "$path") }
rules["common"] = { name -> listOf(":$name" to "modules/common/$name") }
rules["feature"] = { name ->
    if (name.contains("/")) {
        listOf(":${name.replace("/", "_")}" to "modules/feature/${name}")
    } else {
        listOf(
            ":${name}_api" to "modules/feature/$name/api",
            ":${name}_impl" to "modules/feature/$name/impl",
        )
    }
}

rules.keys.forEach { extensions.extraProperties[it] = mutableListOf<String>() }