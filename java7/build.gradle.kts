import net.ltgt.gradle.errorprone.errorprone

buildscript {
    extra.apply {
        set("moduleName", "io.perfmark.javaseven")
    }
}

description = "PerfMark Java7 API"

val jdkVersion = JavaVersion.VERSION_1_8

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()

    options.compilerArgs.add("-Xlint:-options")
}

sourceSets {
    create("jmh")
}

// The java plugin creates these for the 'jmh' source set; the Groovy original did not
// extendFrom(implementation/annotationProcessor), so neither does this - the needed
// dependencies are declared explicitly below.
val jmhImplementation = configurations.getByName("jmhImplementation")

val jmhAnnotationProcessor = configurations.getByName("jmhAnnotationProcessor")

tasks.named<JavaCompile>("compileJmhJava") {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of("11"))
    })
    options.errorprone.excludedPaths.set(".*/build/generated/sources/annotationProcessor/.*")
}

dependencies {
    implementation(project(":perfmark-impl"))

    jmhImplementation(project(":perfmark-api"))
    jmhImplementation(project(":perfmark-impl"))
    jmhImplementation(project(":perfmark-java7"))
    jmhImplementation(project(":perfmark-testing"))
    jmhImplementation(libs.junit)
    jmhImplementation(libs.jmhcore)
    jmhAnnotationProcessor(libs.jmhanno)
}

tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/java7**")
}

tasks.named<Jar>("jar") {
    exclude("io/perfmark/java7/Internal*")
}

tasks.register<Test>("jmh") {
    description = "Runs integration tests."
    group = "stress"

    testClassesDirs = sourceSets["jmh"].output.classesDirs
    classpath = sourceSets["jmh"].runtimeClasspath

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of("16"))
    })
    //shouldRunAfter test
}
