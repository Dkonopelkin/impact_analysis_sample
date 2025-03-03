val single: MutableList<String> by extra
val common: MutableList<String> by extra
val feature: MutableList<String> by extra

with(single) {
    add("app")
}

with(feature) {
    add("feature1")
    add("feature2")
}

with(common) {
    add("ui-kit")
    add("core")
    add("base-network")
    add("base-database")
    add("base-device")
}