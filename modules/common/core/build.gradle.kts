plugins {
    id("com.android.library")
}

android {
    namespace = "com.dkonopelkin.core"
    compileSdk = 35
}

dependencies {
    implementation(project(":base-network"))
    implementation(project(":base-device"))
    implementation(project(":base-database"))
}
