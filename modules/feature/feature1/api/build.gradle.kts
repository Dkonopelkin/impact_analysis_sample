plugins {
    id("com.android.library")
}

android {
    namespace = "com.dkonopelkin.feature1.api"
    compileSdk = 35
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui-kit"))
}
