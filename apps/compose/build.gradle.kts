plugins {
    id("hiittimer.application")
    id("co.touchlab.skie") version "0.10.11"

}

android {
    namespace = "com.dangerfield.hiittimer"
}

kotlin {

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.work.runtime)
            implementation(compose.uiTooling)
        }

        commonMain.dependencies {
            // Project dependencies
            api(projects.libraries.core)
            implementation(projects.libraries.ui)
            implementation(projects.libraries.hiittimer)
            implementation(projects.libraries.hiittimer.impl)
            implementation(projects.libraries.flowroutines)
            implementation(projects.libraries.navigation)
            implementation(projects.libraries.navigation.impl)
            implementation(projects.libraries.resources)

            implementation(projects.libraries.storage)
            implementation(projects.libraries.storage.impl)
            implementation(projects.libraries.hiittimer.storage)
            implementation(projects.libraries.config)
            implementation(projects.libraries.config.impl)
            implementation(projects.libraries.hiittimer.storage)

            implementation(projects.features.home)
            implementation(projects.features.home.impl)
            implementation(projects.features.timers)
            implementation(projects.features.timers.impl)
            implementation(projects.features.timers.storage)
            implementation(projects.features.settings)
            implementation(projects.features.settings.impl)

            implementation(projects.libraries.preferences)
            implementation(projects.libraries.preferences.impl)
            implementation(projects.libraries.review)
            implementation(projects.libraries.review.impl)
            implementation(projects.libraries.inappmessages)
            implementation(projects.libraries.inappmessages.impl)

            implementation(libs.atomicfu)
            
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
        }
    }
}