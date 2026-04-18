package com.dangerfield.hiittimer.plugin

import com.android.build.gradle.LibraryExtension
import com.dangerfield.hiittimer.ext.ConfigurationExtension
import com.dangerfield.hiittimer.util.configureAndroid
import com.dangerfield.hiittimer.util.configureKotlinInject
import com.dangerfield.hiittimer.util.configureKotlinMultiplatform
import com.dangerfield.hiittimer.util.libs
import com.dangerfield.hiittimer.util.optInKotlinMarkers
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin for library modules that need Compose UI capabilities.
 * 
 * **When to use this plugin:**
 * - UI component libraries that provide reusable Compose components
 * - Libraries that contain Compose UI utilities and extensions
 * - Any library module that needs to create or manipulate Compose UI
 * - Modules that provide design system components
 * 
 * **What this plugin provides:**
 * - Full Kotlin Multiplatform setup (Android, iOS, JVM targets)
 * - Compose Multiplatform and Compose Compiler plugins
 * - Android library configuration
 * - Common KMP dependencies (coroutines, kotlin-test)
 * - Compose runtime test dependencies
 * 
 * **Examples of modules that should use this:**
 * - libraries:ui (your UI component library)
 * - libraries:design-system (if you have one)
 * - Any library that exports @Composable functions
 * 
 * **Don't use this plugin for:**
 * - Feature modules with screens and navigation (use hiittimer.feature instead)
 * - Pure Kotlin libraries without UI (use hiittimer.kotlin.multiplatform instead)
 * - The main application module (use hiittimer.android.application instead)
 */
class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.library")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            project.optInKotlinMarkers("kotlin.time.ExperimentalTime")
            project.optInKotlinMarkers("kotlin.uuid.ExperimentalUuidApi")

            configureKotlinMultiplatform()
            configureKotlinInject()
            configureComposeTestDependencies()
            
            extensions.configure<LibraryExtension> {
                configureAndroid()
            }

            if (extensions.findByName("moduleConfig") == null) {
                extensions.create("moduleConfig", ConfigurationExtension::class.java)
            }
        }
    }
    
    private fun Project.configureComposeTestDependencies() {
        // Add compose runtime dependency for test source sets to satisfy compose compiler
        dependencies {
            add("commonTestImplementation", "org.jetbrains.compose.runtime:runtime:1.9.1")
        }
    }
    

}