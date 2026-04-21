enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "Rounds"

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
    }
}

// Apps
include(":apps")
include(":apps:compose")
// Note: iOS app is not a Gradle module - it's an Xcode project in apps/ios/
// Note: Desktop app module exists but may need to be configured

// Features
include(":features:home")
include(":features:home:impl")
include(":features:settings")
include(":features:settings:impl")
include(":features:timers")
include(":features:timers:impl")
include(":features:timers:storage")


// Libraries
include(":libraries:config")
include(":libraries:config:impl")
include(":libraries:core")
include(":libraries:flowroutines")
include(":libraries:navigation")
include(":libraries:navigation:impl")
include(":libraries:preferences")
include(":libraries:preferences:impl")
include(":libraries:resources")
include(":libraries:review")
include(":libraries:review:impl")
include(":libraries:inappmessages")
include(":libraries:inappmessages:impl")
include(":libraries:storage")
include(":libraries:storage:impl")
include(":libraries:hiittimer")
include(":libraries:hiittimer:impl")
include(":libraries:hiittimer:storage")
include(":libraries:ui")