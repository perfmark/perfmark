@Suppress("DSL_SCOPE_VIOLATION") // See https://github.com/gradle/gradle/issues/22797
plugins {
    alias(libs.plugins.jmh)
    alias(libs.plugins.spotless)
}

buildscript {
    extra.apply{
        set("moduleName", "io.perfmark")
    }
}


val jdkVersion = JavaVersion.VERSION_1_6

description = "PerfMark API"

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

    jmhImplementation(project(":perfmark-java9"))
    jmhImplementation(project(":perfmark-java7"))
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
}

java {
    disableAutoTargetJvm()
}

tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/Impl**")
}


jmh {

    timeOnIteration.set("1s")
    warmup.set("1s")
    fork.set(400)
    warmupIterations.set(0)

    includes.add("ClassInit")
    profilers.add("cl")
    jvmArgs.add("-Dio.perfmark.debug=true")

    /*
    profilers = ["perfasm"]

    jvmArgs = [
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+LogCompilation",
            "-XX:LogFile=/tmp/blah.txt",
            "-XX:+PrintAssembly",
            "-XX:+PrintInterpreter",
            "-XX:+PrintNMethods",
            "-XX:+PrintNativeNMethods",
            "-XX:+PrintSignatureHandlers",
            "-XX:+PrintAdapterHandlers",
            "-XX:+PrintStubCode",
            "-XX:+PrintCompilation",
            "-XX:+PrintInlining",
            "-XX:+TraceClassLoading",
            "-XX:PrintAssemblyOptions=syntax",
            "-XX:PrintAssemblyOptions=intel"
    ]
     */

    //duplicateClassesStrategy DuplicatesStrategy.INCLUDE
}