plugins {
    id("com.android.library")
}

android {
    namespace = "com.dkonopelkin.feature2.api"
    compileSdk = 35
}

dependencies {
    implementation(project(":ui-kit"))
}
