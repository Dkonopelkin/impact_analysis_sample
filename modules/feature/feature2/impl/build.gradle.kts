plugins {
    id("com.android.library")
}

android {
    namespace = "com.dkonopelkin.feature2.impl"
    compileSdk = 35
}

dependencies {
    implementation(project(":ui-kit"))
    implementation(project(":feature2_api"))
    implementation(project(":feature1_api"))
}
