plugins {
    id("hiittimer.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.hiittimer.libraries.inappmessages"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.preferences)
            implementation(libs.kotlin.inject.runtime.kmp)
        }
    }
}
