import groovy.util.Node

buildscript {
    extra.apply {
        set("moduleName", "io.perfmark.agent")
    }
}


plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val jdkVersion = JavaVersion.VERSION_1_6

dependencies {
    val libraries = project.ext.get("libraries") as Map<String, String>

    compileOnly(libraries["jsr305"]!!)
    compileOnly(libraries["errorprone"]!!)

    implementation("org.ow2.asm:asm:9.1")
    implementation("org.ow2.asm:asm-commons:9.1")

    testImplementation(project(":perfmark-api"))
    testImplementation(libraries["truth"]!!)
    testImplementation(project(":perfmark-impl"))
    testRuntimeOnly(project(":perfmark-java6"))
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()

    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(11))
    })

    options.compilerArgs.add("-Xlint:-options")
}

tasks.named<JavaCompile>("compileTestJava") {
    sourceCompatibility = JavaVersion.VERSION_16.toString()
    targetCompatibility = JavaVersion.VERSION_16.toString()
}

tasks.named<Jar>("jar") {
    archiveClassifier.value("original")
    manifest {
        attributes(mapOf(
                "Premain-Class" to "io.perfmark.agent.Main",
        ))
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    // make sure this is THE jar, which removes the suffix.
    archiveClassifier.value(null as String?)

    relocate("org.objectweb.asm", "io.perfmark.agent.shaded.org.objectweb.asm")
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["sourcesElements"]) {
    skip()
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifacts.removeIf {
                it.classifier.toString().contains("original")
            }
            artifact(tasks["shadowJar"])
            artifact(tasks["sourcesJar"])

            pom.withXml {
                val root = asNode()

                for (child in root.children()) {
                    val c = child as Node
                    if (c.name().toString().endsWith("dependencies")) {
                        root.remove(c)
                        break
                    }
                }
            }
        }

        /*
        maven(MavenPublication) {
            // Ideally swap to project.shadow.component(it) when it isn't broken for project deps
            artifact shadowJar
                    // Empty jars are not published via withJavadocJar() and withSourcesJar()
                    artifact javadocJar
                    artifact sourcesJar

                    pom.withXml {
                        def dependencies = asNode().appendNode('dependencies')
                        project.configurations.shadow.allDependencies.each { dep ->
                            def dependencyNode = dependencies.appendNode('dependency')
                            dependencyNode.appendNode('groupId', dep.group)
                            dependencyNode.appendNode('artifactId', dep.name)
                            def version = (dep.name == 'grpc-core') ? '[' + dep.version + ']' : dep.version
                            dependencyNode.appendNode('version', version)
                            dependencyNode.appendNode('scope', 'compile')
                        }
                    }
        }*/
    }
}