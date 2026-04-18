plugins {
    id("hiittimer.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.hiittimer.libraries.review"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(libs.kotlin.inject.runtime.kmp)
        }
    }
}
