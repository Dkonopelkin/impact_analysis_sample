plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.dkonopelkin.feature2.impl"
    compileSdk = 35
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":ui-kit"))
    implementation(project(":feature2_api"))
    implementation(project(":feature1_api"))
    implementation(libs.androidx.core.ktx)
}
