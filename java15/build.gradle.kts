import net.ltgt.gradle.errorprone.errorprone

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.jcstress)
}

buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.javafifteen")
    }
}

val jdkVersion = JavaVersion.VERSION_15

description = "PerfMark Java15 API"

sourceSets {
    create("jmh")
}

val jmhImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val jmhAnnotationProcessor by configurations.getting {
    extendsFrom(configurations.annotationProcessor.get())
}

dependencies {
    implementation(project(":perfmark-impl"))

    testImplementation(project(":perfmark-api"))
    testImplementation(project(":perfmark-testing"))
    jcstressImplementation(project(":perfmark-impl"))

    jmhImplementation(project(":perfmark-api"))
    jmhImplementation(project(":perfmark-impl"))
    jmhImplementation(project(":perfmark-java15"))
    jmhImplementation(project(":perfmark-testing"))

    jmhImplementation(libs.junit)
    jmhImplementation(libs.jmhcore)
    jmhAnnotationProcessor(libs.jmhanno)
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
}

tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/java15/**")
}

tasks.register<Test>("jmh") {
    description = "Runs integration tests."
    group = "stress"

    testClassesDirs = sourceSets["jmh"].output.classesDirs
    classpath = sourceSets["jmh"].runtimeClasspath
}

//  ./gradlew --no-daemon clean :perfmark-java9:jcstress
jcstress {
    jcstressDependency = "org.openjdk.jcstress:jcstress-core:0.15"
    // mode "tough"
    deoptRatio = "2"
}


tasks.named<JavaCompile>("compileJmhJava") {
    options.errorprone.excludedPaths.set(".*/build/generated/sources/annotationProcessor/.*")
}


tasks.named<JavaCompile>("compileJcstressJava") {
    options.errorprone.excludedPaths.set(".*/build/generated/sources/annotationProcessor/.*")
}
