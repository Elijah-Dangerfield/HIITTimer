package com.dangerfield.hiittimer.plugin

import com.android.build.api.dsl.ApplicationExtension
import com.dangerfield.hiittimer.ext.ConfigurationExtension
import com.dangerfield.hiittimer.util.SharedConstants
import com.dangerfield.hiittimer.util.configureAndroid
import com.dangerfield.hiittimer.util.configureKotlinInject
import com.dangerfield.hiittimer.util.configureReleaseSigning
import com.dangerfield.hiittimer.util.verifyGitHooksInstalled
import com.dangerfield.hiittimer.util.configureKotlinMultiplatform
import com.dangerfield.hiittimer.util.libs
import com.dangerfield.hiittimer.util.loadVersionMetadata
import com.dangerfield.hiittimer.util.optInKotlinMarkers
import com.dangerfield.hiittimer.util.VersionMetadata
import com.dangerfield.hiittimer.util.writeCommonMetadata
import com.github.gmazzo.buildconfig.BuildConfigExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for the main Android application module.
 *
 * **When to use this plugin:**
 * - The main app module that gets installed on devices
 * - The module that contains MainActivity and app-level configuration
 * - The module that defines the applicationId and app metadata
 *
 * **What this plugin provides:**
 * - Android application plugin configuration
 * - Kotlin Multiplatform setup with Android and iOS targets
 * - Compose and Compose Compiler plugins
 * - iOS framework configuration for KMP
 * - Application-specific build configuration (version codes, signing, etc.)
 * - Activity Compose dependencies
 *
 * **Examples of modules that should use this:**
 * - apps:compose (your main app)
 * - apps:desktop (if you have a desktop app variant)
 *
 * **Don't use this plugin for:**
 * - Feature modules (use hiittimer.feature instead)
 * - Library modules (use hiittimer.compose.multiplatform or hiittimer.kotlin.multiplatform)
 * - Server modules (these wouldn't be Android applications)
 */
class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            verifyGitHooksInstalled()
            val versionMetadata = loadVersionMetadata()
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.application")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply(libs.plugins.kotlinSerialization.get().pluginId)
                apply(libs.plugins.buildconfig.get().pluginId)
            }

            project.optInKotlinMarkers("kotlin.time.ExperimentalTime")
            project.optInKotlinMarkers("kotlin.uuid.ExperimentalUuidApi")

            configureKotlinMultiplatform {
                binaries.framework {
                    baseName = "ComposeApp"
                    isStatic = true
                    binaryOption("bundleId", "com.dangerfield.hiittimer")
                    export(project(":libraries:core"))
                }
            }
            configureKotlinInject()

            extensions.configure<ApplicationExtension> {
                configureAndroid()

                defaultConfig {
                    applicationId = versionMetadata.applicationId
                    targetSdk = SharedConstants.targetSdk
                    versionCode = versionMetadata.versionCode
                    versionName = versionMetadata.versionName
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }

                val releaseSigning = configureReleaseSigning(this)

                buildTypes {
                    debug {
                        applicationIdSuffix = ".debug"
                    }
                    release {
                        isMinifyEnabled = false
                        signingConfig = releaseSigning ?: signingConfigs.getByName("debug")
                    }
                }
            }

            if (extensions.findByName("moduleConfig") == null) {
                extensions.create("moduleConfig", ConfigurationExtension::class.java)
            }
            configureAppBuildConfig(versionMetadata)
        }
    }

    private fun Project.configureAppBuildConfig(metadata: VersionMetadata) {
        extensions.configure(BuildConfigExtension::class.java) {
            // Kotlin package for the generated BuildConfig class — independent
            // of Android applicationId so renaming the applicationId doesn't
            // break import paths in the app.
            packageName("com.dangerfield.hiittimer.appconfig")
            className("AppBuildConfig")
            useKotlinOutput {
                internalVisibility = false
            }
            writeCommonMetadata(metadata)
        }
    }
}