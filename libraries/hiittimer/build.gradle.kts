plugins {
    id("hiittimer.kotlin.multiplatform")
}

moduleConfig {
    optIn("kotlin.time.ExperimentalTime")
    serialization()
}

android {
    namespace = "com.dangerfield.hiittimer.libraries.hiittimer"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            api(projects.libraries.storage)
            implementation(libs.configuration.annotations)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.process)
        }
    }
}