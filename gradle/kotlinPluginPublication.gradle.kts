apply(plugin = "maven-publish")

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("KotlinPlugin") {
            val artifactName = if (project.name == "idea-plugin") "kotlin-plugin" else project.name
            artifactId = "$artifactName-${IdeVersionConfigurator.currentIde.name.toLowerCase()}"
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