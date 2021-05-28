buildscript {
    extra.apply {
        set("moduleName", "io.perfmark.agent")
    }
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
    manifest {
        attributes(mapOf(
                "Premain-Class" to "io.perfmark.agent.Main",
        ))
    }
}
