import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("me.champeau.jmh")
    id("io.github.reyerizo.gradle.jcstress")
}

buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.javanine")
    }
}

val jdkVersion = JavaVersion.VERSION_15

description = "PerfMark Java15 API"


dependencies {
    val libraries = project.ext.get("libraries") as Map<String, String>

    implementation(project(":perfmark-impl"))
    compileOnly(libraries["jsr305"]!!)

    testImplementation(project(":perfmark-api"))
    testImplementation(project(":perfmark-testing"))
    jcstressImplementation(project(":perfmark-impl"))
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
}


tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/java15/**")
}

//  ./gradlew --no-daemon clean :perfmark-java9:jcstress
jcstress {
    jcstressDependency = "org.openjdk.jcstress:jcstress-core:0.5"
    // mode "tough"
    deoptRatio = "2"
}

jmh {
    timeOnIteration.set("1s")
    warmup.set("1s")
    fork.set(1)
    warmupIterations.set(10)

    jvmArgs.addAll("-XX:+UseZGC -Xms2g -Xmx2g -XX:+UnlockDiagnosticVMOptions -XX:PrintAssemblyOptions=syntax -XX:PrintAssemblyOptions=intel")

    profilers.add("perfasm")
    /*
    *     jvmArgs = [
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
    * */
}

tasks.named<JavaCompile>("compileJcstressJava") {
    options.errorprone.excludedPaths.set(".*/build/generated/sources/annotationProcessor/.*")
}
