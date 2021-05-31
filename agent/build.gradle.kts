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
    // Make this not the default
    archiveClassifier.value("original")
    manifest {
        attributes(mapOf(
                "Premain-Class" to "io.perfmark.agent.PerfMarkAgent",
        ))
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    // make sure this is THE jar, which removes the suffix.
    archiveClassifier.value(null as String?)

    relocate("org.objectweb.asm", "io.perfmark.agent.shaded.org.objectweb.asm")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
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
    }
}