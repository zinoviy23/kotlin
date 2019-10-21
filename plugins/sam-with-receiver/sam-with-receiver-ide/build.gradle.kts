
description = "Kotlin SamWithReceiver IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-sam-with-receiver-compiler-plugin"))

    compileOnly(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":idea:idea-core"))

    Ide.IJ183 {
        compileOnly(project(":idea:idea-android"))
    }

    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-jvm"))

    compileOnly(intellijDep()) { includeJars("platform-api", "openapi", "extensions", "util") }
    compileOnly(intellijDep("gradle"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
