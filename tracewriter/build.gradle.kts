buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.tracewriter")
    }
    project.extra.set("libraries", extra.get("libraries"))
}


description = "PerfMark Tracer Output"

val jdkVersion = JavaVersion.VERSION_1_7

tasks.getByName<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()

    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    val libraries = project.extra.get("libraries") as Map<String, String>

    api(project(":perfmark-impl"))
    // Included because it's easy to forget
    runtimeOnly(project(":perfmark-java6"))

    implementation(project(":perfmark-api"))
    implementation(libraries["gson"]!!)

    compileOnly(libraries["jsr305"]!!)
    compileOnly(libraries["errorprone"]!!)
}