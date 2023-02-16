buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.tracewriter")
    }
}

description = "PerfMark Tracer Output"

val jdkVersion = JavaVersion.VERSION_1_7

dependencies {
    api(project(":perfmark-impl"))
    // Included because it's easy to forget
    runtimeOnly(project(":perfmark-java6"))

    implementation(project(":perfmark-api"))
    implementation("com.google.code.gson:gson:2.9.0")

    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")

    compileOnly(libs.jsr305)
    compileOnly(libs.errorprone)
}

tasks.getByName<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
    options.compilerArgs.add("-Xlint:-options")
}
tasks.getByName<JavaCompile>("compileTestJava") {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}