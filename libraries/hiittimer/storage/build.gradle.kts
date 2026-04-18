plugins {
    id("hiittimer.kotlin.multiplatform")
}

android {
    namespace = "com.dangerfield.hiittimer.libraries.hiittimer.storage"
}

moduleConfig.storage()


kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.hiittimer)
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.storage)
        }
    }
}