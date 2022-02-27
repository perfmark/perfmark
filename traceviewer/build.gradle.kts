buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.traceviewer")
    }
}

description = "PerfMark Trace Viewer"

val jdkVersion = JavaVersion.VERSION_1_8

dependencies {
    compileOnly(libs.jsr305)
    compileOnly(libs.errorprone)
    implementation(project(":perfmark-tracewriter"))
    testImplementation(project(":perfmark-api"))
}

tasks.getByName<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
    options.compilerArgs.add("-Xlint:-options")
}
