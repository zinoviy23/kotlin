
description = "Kotlin NoArg Compiler Plugin"

plugins {
    kotlin("jvm")
    `maven-publish`
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }
    runtime(kotlinStdlib())
    testRuntimeOnly(intellijDep()) {
        includeJars("guava", rootProject = rootProject)
    }
    testRuntimeOnly(project(":kotlin-compiler"))

    Platform[192].orHigher {
        testRuntimeOnly(intellijDep()) { includeJars("platform-concurrency") }
    }
    
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()

testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
}

publishing {
    publications {
        create<MavenPublication>("KotlinPlugin") {
            from(components["java"])
        }
    }

    repositories {
        maven(findProperty("deployRepoUrl") ?: "${rootProject.buildDir}/repo")
    }
}

// Disable default `publish` task so publishing will not be done during maven artifact publish
// We should use specialized tasks since we have multiple publications in project
tasks.named("publish") {
    enabled = false
    dependsOn.clear()
}
