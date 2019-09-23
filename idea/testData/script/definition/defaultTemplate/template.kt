package org.jetbrains.kotlin.idea.script

import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.util.PropertiesCollection

@KotlinScript(
    displayName = "Definition for tests",
    fileExtension = "kts",
    compilationConfiguration = TemplateDefinition::class
)
open class Template(val args: Array<String>)

val ScriptingHostConfigurationKeys.getEnvironment by PropertiesCollection.key<() -> Map<String, Any?>?>()

object TemplateDefinition : ScriptCompilationConfiguration(
    {
        jvm {
            dependenciesFromClassContext(TemplateDefinition::class)
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        refineConfiguration {
            beforeCompiling { context ->
                val environment = context.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]?.let {
                    it[ScriptingHostConfiguration.getEnvironment]?.invoke()
                }.orEmpty()

                ScriptCompilationConfiguration(context.compilationConfiguration) {
                    val javaHome = (environment["javaHome"] as? File)
                    if (javaHome != null) {
                        jvm.jdkHome.put(javaHome)
                    }
                    val imports = (environment["imports"] as? List<String>).orEmpty()
                    for (entry in imports) {
                        defaultImports.append(entry)
                    }
                    val classPath = (environment["classpath"] as? List<File>).orEmpty()
                    for (entry in classPath) {
                        dependencies.append(JvmDependency(entry))
                    }
                    val sources = (environment["sources"] as? List<File>).orEmpty()
                    for (entry in sources) {
                        ide.dependenciesSources.append(JvmDependency(entry))
                    }
                }.asSuccess()
            }
        }
    }
)

