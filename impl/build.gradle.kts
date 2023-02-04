plugins {
    id("me.champeau.jmh")
}

buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.impl")
    }
}

val jdkVersion = JavaVersion.VERSION_1_6

description = "PerfMark Implementation API"


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
    implementation(project(":perfmark-api"))
    compileOnly(libs.jsr305)
    compileOnly(libs.errorprone)
    testImplementation(libs.truth)
    testCompileOnly(libs.errorprone)
}


jmh {

    timeOnIteration.set("1s")
    warmup.set("1s")
    fork.set(400)
    warmupIterations.set(0)

    includes.add("ClassInit")
    profilers.add("cl")
    jvmArgs.add("-Dio.perfmark.PerfMark.debug=true")

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