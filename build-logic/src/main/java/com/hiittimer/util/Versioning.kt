package com.dangerfield.hiittimer.util

import com.github.gmazzo.buildconfig.BuildConfigExtension
import org.gradle.api.Project
import java.io.FileInputStream
import java.util.Properties

private const val DEFAULT_APPLICATION_ID = "com.dangerfield.hiittimer"
private const val DEFAULT_VERSION_NAME = "0.0.1"
private const val DEFAULT_VERSION_CODE = 1
private const val DEFAULT_RELEASE_CHANNEL = "dev"
private const val DEFAULT_BUILD_NUMBER = 1

data class VersionMetadata(
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
    val releaseChannel: String,
    val buildNumber: Int
) {
    val releaseDisplay: String = "$versionName ($buildNumber)"
}

fun Project.loadVersionMetadata(): VersionMetadata {
    val properties = Properties()
    val metadataFile = rootProject.file("versions.properties")
    if (metadataFile.exists()) {
        FileInputStream(metadataFile).use(properties::load)
    }

    fun Properties.string(key: String, defaultValue: String): String =
        getProperty(key)?.takeIf { it.isNotBlank() } ?: defaultValue

    fun Properties.int(key: String, defaultValue: Int): Int =
        string(key, defaultValue.toString()).toIntOrNull() ?: defaultValue

    fun env(key: String): String? = System.getenv(key)?.takeIf { it.isNotBlank() }

    val applicationId = properties.string("applicationId", DEFAULT_APPLICATION_ID)
    val versionName = properties.string("versionName", DEFAULT_VERSION_NAME)
    val versionCode = env("VERSION_CODE_OVERRIDE")?.toIntOrNull()
        ?: properties.int("versionCode", DEFAULT_VERSION_CODE)
    val releaseChannel = env("RELEASE_CHANNEL_OVERRIDE")
        ?: properties.string("releaseChannel", DEFAULT_RELEASE_CHANNEL)
    val buildNumber = env("BUILD_NUMBER_OVERRIDE")?.toIntOrNull()
        ?: properties.int("buildNumber", DEFAULT_BUILD_NUMBER)

    return VersionMetadata(
        applicationId = applicationId,
        versionName = versionName,
        versionCode = versionCode,
        releaseChannel = releaseChannel,
        buildNumber = buildNumber
    )
}

fun BuildConfigExtension.writeCommonMetadata(metadata: VersionMetadata) {
    buildConfigField("String", "APPLICATION_ID", "\"${metadata.applicationId}\"")
    buildConfigField("String", "VERSION_NAME", "\"${metadata.versionName}\"")
    buildConfigField("Int", "VERSION_CODE", metadata.versionCode.toString())
    buildConfigField("String", "RELEASE_CHANNEL", "\"${metadata.releaseChannel}\"")
    buildConfigField("Int", "BUILD_NUMBER", metadata.buildNumber.toString())
}
