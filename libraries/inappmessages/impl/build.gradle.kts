plugins {
    id("hiittimer.compose.multiplatform")
}

android {
    namespace = "com.dangerfield.hiittimer.libraries.inappmessages.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.libraries.core)
            implementation(projects.libraries.inappmessages)
            implementation(projects.libraries.preferences)
            implementation(projects.libraries.review)
            implementation(projects.libraries.navigation)
            implementation(projects.libraries.resources)
            implementation(projects.libraries.ui)
            implementation(projects.features.timers)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlin.inject.runtime.kmp)
        }
    }
}
