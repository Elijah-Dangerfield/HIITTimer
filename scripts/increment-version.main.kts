#!/usr/bin/env kotlin

import java.io.File

private val RED = "\u001b[31m"
private val GREEN = "\u001b[32m"
private val YELLOW = "\u001b[33m"
private val RESET = "\u001b[0m"

fun printGreen(text: String) = println("$GREEN$text$RESET")
fun printYellow(text: String) = println("$YELLOW$text$RESET")
fun printRed(text: String) = println("$RED$text$RESET")

fun usage(): Nothing {
    printRed(
        """
        Usage: increment-version.main.kts <bump>

          bump = patch | minor | major | <explicit x.y.z>

        Side effects:
          - versions.properties: bumps versionName, +1 versionCode, +1 buildNumber
          - apps/ios/Configuration/Config.xcconfig: MARKETING_VERSION mirrors versionName

        Prints the new version to stdout as `version=x.y.z`.
        """.trimIndent()
    )
    kotlin.system.exitProcess(1)
}

val arg = args.firstOrNull() ?: usage()

data class SemVer(val major: Int, val minor: Int, val patch: Int) {
    override fun toString() = "$major.$minor.$patch"
    fun bumpPatch() = copy(patch = patch + 1)
    fun bumpMinor() = copy(minor = minor + 1, patch = 0)
    fun bumpMajor() = copy(major = major + 1, minor = 0, patch = 0)
}

fun parseSemVer(raw: String): SemVer {
    val parts = raw.trim().split(".")
    require(parts.size == 3) { "Version must be x.y.z, got: $raw" }
    return SemVer(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}

val repoRoot = File(".").absoluteFile.canonicalFile.let { start ->
    generateSequence(start) { it.parentFile }
        .firstOrNull { File(it, "settings.gradle.kts").exists() }
        ?: error("Could not find repo root (no settings.gradle.kts in any parent)")
}

val propsFile = File(repoRoot, "versions.properties")
require(propsFile.exists()) { "Missing versions.properties at ${propsFile.absolutePath}" }

val xcconfigFile = File(repoRoot, "apps/ios/Configuration/Config.xcconfig")
require(xcconfigFile.exists()) { "Missing Config.xcconfig at ${xcconfigFile.absolutePath}" }

val originalLines = propsFile.readLines()
val currentProps = originalLines
    .filter { it.isNotBlank() && !it.startsWith("#") }
    .associate { line ->
        val idx = line.indexOf('=')
        line.substring(0, idx).trim() to line.substring(idx + 1).trim()
    }

val currentVersion = parseSemVer(currentProps["versionName"] ?: error("versionName missing"))
val currentVersionCode = (currentProps["versionCode"] ?: error("versionCode missing")).toInt()
val currentBuildNumber = (currentProps["buildNumber"] ?: error("buildNumber missing")).toInt()

val newVersion = when (arg) {
    "patch" -> currentVersion.bumpPatch()
    "minor" -> currentVersion.bumpMinor()
    "major" -> currentVersion.bumpMajor()
    else -> parseSemVer(arg)
}
val newVersionCode = currentVersionCode + 1
val newBuildNumber = currentBuildNumber + 1

printYellow("Current: versionName=$currentVersion versionCode=$currentVersionCode buildNumber=$currentBuildNumber")
printGreen("New:     versionName=$newVersion versionCode=$newVersionCode buildNumber=$newBuildNumber")

val updatedLines = originalLines.map { line ->
    when {
        line.startsWith("versionName=") -> "versionName=$newVersion"
        line.startsWith("versionCode=") -> "versionCode=$newVersionCode"
        line.startsWith("buildNumber=") -> "buildNumber=$newBuildNumber"
        else -> line
    }
}
propsFile.writeText(updatedLines.joinToString("\n") + "\n")

val xcconfigLines = xcconfigFile.readLines().map { line ->
    if (line.trimStart().startsWith("MARKETING_VERSION")) {
        val prefix = line.substringBefore("MARKETING_VERSION")
        "${prefix}MARKETING_VERSION=$newVersion"
    } else {
        line
    }
}
xcconfigFile.writeText(xcconfigLines.joinToString("\n") + "\n")

printGreen("Updated ${propsFile.relativeTo(repoRoot)}")
printGreen("Updated ${xcconfigFile.relativeTo(repoRoot)}")

val githubOutput = System.getenv("GITHUB_OUTPUT")
if (githubOutput != null) {
    File(githubOutput).appendText(
        "version=$newVersion\n" +
            "versionCode=$newVersionCode\n" +
            "buildNumber=$newBuildNumber\n"
    )
    printGreen("Wrote outputs to \$GITHUB_OUTPUT")
}

println("version=$newVersion")
