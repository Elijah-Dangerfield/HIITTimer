plugins {
    id("hiittimer.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.hiittimer.libraries.config"
}


kotlin {
    sourceSets {
        commonMain.dependencies {
            
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(libs.kotlin.inject.runtime.kmp)
        }
    }
}