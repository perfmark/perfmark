import net.ltgt.gradle.errorprone.errorprone

@Suppress("DSL_SCOPE_VIOLATION") // See https://github.com/gradle/gradle/issues/22797
plugins {
    alias(libs.plugins.spotless)
}

buildscript {
    extra.apply{
        set("moduleName", "io.perfmark")
    }
}


val jdkVersion = JavaVersion.VERSION_1_6

description = "PerfMark API"

sourceSets {
    create("jmh")
}

val jmhImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val jmhAnnotationProcessor by configurations.getting {
    extendsFrom(configurations.annotationProcessor.get())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()

    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    compileOnly(libs.errorprone)

    testImplementation(project(":perfmark-impl"))
    testImplementation(libs.truth)
    testRuntimeOnly(project(":perfmark-java6"))

    jmhImplementation(project(":perfmark-api"))
    jmhImplementation(project(":perfmark-java9"))
    jmhImplementation(project(":perfmark-java7"))
    jmhImplementation(libs.junit)
    jmhImplementation(libs.jmhcore)
    jmhAnnotationProcessor(libs.jmhanno)
}

spotless {
    java {
        googleJavaFormat(libs.versions.gjf.get())
    }
}

tasks.named<JavaCompile>("compileTestJava") {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}


tasks.named<JavaCompile>("compileJmhJava") {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
    options.errorprone.excludedPaths.set(".*/build/generated/sources/annotationProcessor/.*")
}

java {
    // disableAutoTargetJvm()
}

tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/Impl**")
}

tasks.named<JavaCompile>("compileJmhJava") {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}

tasks.register<Test>("jmh") {
    description = "Runs integration tests."
    group = "stress"

    testClassesDirs = sourceSets["jmh"].output.classesDirs
    classpath = sourceSets["jmh"].runtimeClasspath

    javaLauncher.set(javaToolchains.launcherFor({
        languageVersion.set(JavaLanguageVersion.of("11"))
    }))
}