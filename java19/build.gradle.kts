import net.ltgt.gradle.errorprone.errorprone

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.jcstress)
    alias(libs.plugins.spotless)
}

buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.javanineteen")
    }
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
}

val jdkVersion = JavaVersion.VERSION_19

description = "PerfMark Java19 API"

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
    testRuntimeOnly(project(":perfmark-java6"))
    jcstressImplementation(project(":perfmark-impl"))

    jmhImplementation(project(":perfmark-api"))
    jmhImplementation(project(":perfmark-impl"))
    jmhImplementation(project(":perfmark-java19"))
    jmhImplementation(project(":perfmark-testing"))

    jmhImplementation(libs.junit)
    jmhImplementation(libs.jmhcore)
    jmhAnnotationProcessor(libs.jmhanno)
}

spotless {
    java {
        googleJavaFormat(libs.versions.gjf.get())
    }
}


tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
}

tasks.named<JavaCompile>("compileTestJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
    options.compilerArgs.add("--enable-preview")
}

tasks.named<JavaCompile>("compileJmhJava") {
    options.errorprone.excludedPaths.set(".*/build/generated/sources/annotationProcessor/.*")
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
    options.compilerArgs.add("--enable-preview")
    options.compilerArgs.add("-Xlint:preview")
}


tasks.withType<Test>().all {

    jvmArgs("--enable-preview")
}

tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/java19/**")
}

tasks.register<Test>("jmh") {
    description = "Runs integration tests."
    group = "stress"

    testClassesDirs = sourceSets["jmh"].output.classesDirs
    classpath = sourceSets["jmh"].runtimeClasspath
}

//  ./gradlew --no-daemon clean :perfmark-java9:jcstress
jcstress {
    jcstressDependency = "org.openjdk.jcstress:jcstress-core:0.5"
    // mode "tough"
    deoptRatio = "2"
}

tasks.named<JavaCompile>("compileJcstressJava") {
    options.errorprone.excludedPaths.set(".*/build/generated/sources/annotationProcessor/.*")
}
