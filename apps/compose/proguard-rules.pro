# R8/ProGuard rules for Rounds (Android release).
# Keep conservative. Most libraries in this stack ship their own consumer
# rules; the keeps below cover known reflection-based paths that aren't
# always handled upstream.

# --- General Kotlin / JVM ----------------------------------------------------

-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations, RuntimeInvisibleTypeAnnotations
-keepattributes AnnotationDefault

-keep class kotlin.Metadata { *; }

# Coroutines — runtime reflects on the continuation's class file
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- kotlinx.serialization --------------------------------------------------
# Routes + other @Serializable models round-trip through reflection.

-keepclassmembers @kotlinx.serialization.Serializable class * {
    static <1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class **
-keep class <1>$$serializer { *; }

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Navigation / Route sealed hierarchy ------------------------------------
# Route classes are @Serializable and instantiated by the nav library via
# reflection when deep-linking. Keep the whole sealed tree.

-keep class com.dangerfield.hiittimer.libraries.navigation.Route { *; }
-keep class * extends com.dangerfield.hiittimer.libraries.navigation.Route { *; }

# --- Room -------------------------------------------------------------------
# Room ships consumer rules but be explicit for entities/DAOs that the
# generated code instantiates via reflection at runtime.

-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# --- kotlin-inject-anvil ----------------------------------------------------
# Generated component + factories are referenced by name.

-keep class **.MergedAppComponent { *; }
-keep class ** implements me.tatarka.inject.annotations.Component { *; }

# --- Sentry KMP -------------------------------------------------------------
# Ships its own rules, but the Kotlin Multiplatform flavor reflects on
# some data classes for serializing events.

-keep class io.sentry.** { *; }
-keep class io.sentry.kotlin.multiplatform.** { *; }

# --- Compose ----------------------------------------------------------------
# Compose ships rules in androidx.compose.** — shouldn't need more here.
# Keep @Composable function signatures just in case UI inspection is used.

-keepclassmembers class **.*Kt {
    @androidx.compose.runtime.Composable <methods>;
}

# --- BuildConfig classes ----------------------------------------------------
# Generated at build time by build-config plugin. Read by AppInfoImpl via
# direct field access — stripping could break version display.

-keep class com.dangerfield.hiittimer.appconfig.AppBuildConfig { *; }
-keep class com.dangerfield.hiittimer.buildinfo.HIITTimerBuildConfig { *; }

# --- Kotlin Multiplatform / native interop (iOS only, harmless on Android) --
# Kept to ensure any shared-module expect/actual surfaces survive.

-keepnames class com.dangerfield.hiittimer.** { *; }

# --- App Application + Activity classes -------------------------------------
# Referenced from AndroidManifest by fully-qualified name.

-keep class com.dangerfield.hiittimer.HIITTimerApplication { *; }
-keep class com.dangerfield.hiittimer.MainActivity { *; }

# --- Diagnostic aid ---------------------------------------------------------
# If R8 ever strips something critical, line numbers in the Sentry mapping
# help pinpoint it. Don't strip source filenames from exceptions.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
