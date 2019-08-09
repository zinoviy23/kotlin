
description = "Kotlin Gradle Tooling support"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
    compile(intellijPluginDep("gradle"))
    compileOnly(intellijDep()) { includeJars("slf4j-api-1.7.25") }
    compile(files("/Users/sergey.rostov/Downloads/gradle-6.0-branch-eskatos_kotlin_dsl_multi_scripts_resolver-20190919115959+0000/lib/gradle-kotlin-dsl-tooling-models-6.0.jar"))

    compileOnly(project(":kotlin-reflect-api"))
    runtime(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
