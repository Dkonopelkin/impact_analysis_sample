plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.dkonopelkin.core"
    compileSdk = 35
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":base-network"))
    implementation(project(":base-device"))
    implementation(project(":base-database"))
    implementation(libs.androidx.core.ktx)
}
