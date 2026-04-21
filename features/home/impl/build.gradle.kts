plugins {
    id("hiittimer.feature")
}

android {
    namespace = "com.dangerfield.hiittimer.features.home.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.home)
            implementation(projects.features.settings)
            implementation(projects.libraries.navigation)

            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.hiittimer)
            implementation(projects.libraries.resources)

            // Compose dependencies (navigation and lifecycle provided by hiittimer.feature plugin)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
        }
    }
}