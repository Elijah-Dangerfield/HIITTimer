plugins {
    id("hiittimer.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.hiittimer.libraries.core"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            implementation(libs.kotlin.inject.runtime.kmp)
        }
    }
}