buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.traceviewer")
    }
    project.extra.set("libraries", extra.get("libraries"))
}

description = "PerfMark Trace Viewer"

val jdkVersion = JavaVersion.VERSION_1_8

tasks.getByName<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()

    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    val libraries = project.extra.get("libraries") as Map<String, String>
    compileOnly(libraries["jsr305"]!!)
    compileOnly(libraries["errorprone"]!!)
    implementation(project(":perfmark-tracewriter"))
    testImplementation(project(":perfmark-api"))
}