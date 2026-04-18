plugins {
    id("hiittimer.kotlin.multiplatform")
    alias(libs.plugins.sentryKmp)
}

moduleConfig {
    optIn("kotlin.time.ExperimentalTime")
    optIn("kotlin.uuid.ExperimentalUuidApi")
    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
}

android {
    namespace = "com.dangerfield.hiittimer.libraries.hiittimer.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.hiittimer)

            implementation(projects.libraries.core)
            implementation(libs.kermit)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.hiittimer.storage)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
