// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

val opencvVersion = "4.12.0"
val opencvSdkUrl = "https://github.com/opencv/opencv/releases/download/${opencvVersion}/opencv-${opencvVersion}-android-sdk.zip"
val opencvSdkZip = file("opencv/opencv-android-sdk.zip")
val opencvSdkDir = file("opencv/OpenCV-android-sdk")

// Task to download OpenCV SDK
tasks.register("downloadOpenCV") {
    doLast {
        // Ensure the opencv directory exists
        val opencvDir = file("opencv")
        if (!opencvDir.exists()) {
            opencvDir.mkdirs()
        }
        if (!opencvSdkZip.exists()) {
            println("Downloading OpenCV SDK...")
            ant.invokeMethod("get", mapOf(
                "src" to opencvSdkUrl,
                "dest" to opencvSdkZip,
                "verbose" to true
            ))
            println("Downloaded OpenCV SDK to ${opencvSdkZip}")
        } else {
            println("OpenCV SDK zip already exists.")
        }
    }
}

// Task to extract OpenCV SDK
tasks.register("extractOpenCV") {
    dependsOn("downloadOpenCV")
    doLast {
        if (!opencvSdkDir.exists()) {
            println("Extracting OpenCV SDK...")
            copy {
                from(zipTree(opencvSdkZip))
                into("opencv/")
            }
            println("Extracted OpenCV SDK to ${opencvSdkDir}")
        } else {
            println("OpenCV SDK already extracted.")
        }
    }
}

// Fallback: Ensure OpenCV is available before building (kept for safety)
project(":app").tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(":extractOpenCV") // Ensure extractOpenCV runs before preBuild
    doFirst {
        if (!opencvSdkDir.exists()) {
            throw GradleException("OpenCV SDK not found. Please run './gradlew extractOpenCV' or sync the project.")
        }
    }
}

// Make sure OpenCV is downloaded and extracted before build
// You can run './gradlew extractOpenCV' before building, or add 'dependsOn("extractOpenCV")' to relevant build tasks.

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(17)
        }
    }
    // Force JVM toolchain for opencv Android module
    if (name == "opencv") {
        plugins.withId("org.jetbrains.kotlin.android") {
            extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension>("kotlin") {
                jvmToolchain(17)
            }
        }
    }
}

gradle.projectsEvaluated {
    if (!opencvSdkDir.exists()) {
        println("OpenCV SDK not found. Downloading and extracting...")
        tasks.named("downloadOpenCV").get().actions.forEach { it.execute(tasks.named("downloadOpenCV").get()) }
        tasks.named("extractOpenCV").get().actions.forEach { it.execute(tasks.named("extractOpenCV").get()) }
    }
}
