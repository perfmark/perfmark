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

val jdkVersion = JavaVersion.VERSION_1_9

description = "PerfMark Java9 API"


dependencies {
    val libraries = project.ext.get("libraries") as Map<String, String>

    implementation(project(":perfmark-impl"))
    compileOnly(libraries["jsr305"]!!)

    testImplementation(project(":perfmark-api"))
    jcstressImplementation(project(":perfmark-impl"))
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
}

tasks.named<Jar>("jar") {
    exclude("io/perfmark/java9/Internal*")
}

tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/java9/**")
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

