plugins {
    id("hiittimer.feature")
}

android {
    namespace = "com.dangerfield.hiittimer.features.timers.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.timers)
            implementation(projects.features.timers.storage)
            implementation(projects.features.settings)
            implementation(projects.libraries.navigation)

            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.preferences)
            implementation(projects.libraries.review)
            implementation(libs.reorderable)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }
    }
}
