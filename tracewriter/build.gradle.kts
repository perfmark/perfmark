buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.tracewriter")
    }
}

description = "PerfMark Tracer Output"

val jdkVersion = JavaVersion.VERSION_1_7

dependencies {
    val libraries = project.ext.get("libraries") as Map<String, String>

    api(project(":perfmark-impl"))
    // Included because it's easy to forget
    runtimeOnly(project(":perfmark-java6"))

    implementation(project(":perfmark-api"))
    implementation(libraries["gson"]!!)

    compileOnly(libraries["jsr305"]!!)
    compileOnly(libraries["errorprone"]!!)
}

tasks.getByName<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
    options.compilerArgs.add("-Xlint:-options")
}
