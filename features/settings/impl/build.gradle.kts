plugins {
    id("hiittimer.feature")
}

android {
    namespace = "com.dangerfield.hiittimer.features.settings.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.settings)
            implementation(projects.features.timers)
            implementation(projects.libraries.navigation)

            implementation(projects.libraries.core)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.preferences)
            implementation(projects.libraries.review)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
    }
}
