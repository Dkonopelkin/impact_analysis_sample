plugins {
    id("com.android.library")
}

android {
    namespace = "com.dkonopelkin.feature1.impl"
    compileSdk = 35
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui-kit"))
    implementation(project(":feature1_api"))
}
