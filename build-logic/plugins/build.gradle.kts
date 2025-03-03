plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins.register("com.dkonopelkin.impact-analysis") {
        id = "com.dkonopelkin.impact-analysis"
        implementationClass = "com.dkonopelkin.android.impact.analysis.ImpactAnalysisPlugin"
    }
}

dependencies {
    implementation(libs.utils.jdom2)
}