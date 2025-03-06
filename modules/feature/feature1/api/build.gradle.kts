plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.dkonopelkin.feature1.api"
    compileSdk = 35
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui-kit"))
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
