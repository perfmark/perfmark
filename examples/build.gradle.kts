plugins {
    application
}

buildscript {
    extra.apply {
        set("moduleName", "io.perfmark.examples")
    }
}

val jdkVersion = JavaVersion.VERSION_1_8


configurations {
    create("perfmarkAgent")
}

dependencies {
    implementation(project(":perfmark-api"))
    implementation(project(":perfmark-tracewriter"))
    runtimeOnly(project(":perfmark-java7"))
    runtimeOnly(project(":perfmark-java6"))

    add("perfmarkAgent", project(":perfmark-agent", configuration = "shadow"))
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
}

tasks.named<JavaExec>("run") {
    dependsOn(":perfmark-agent:shadowJar")
}

application {
    mainClass.set("io.perfmark.examples.perfetto.WebServer")
    applicationDefaultJvmArgs = mutableListOf(
            "-javaagent:" + configurations.getByName("perfmarkAgent").singleFile.path,
            "-Xlog:class+load=info",
            "-XX:StartFlightRecording",
            "-Dio.perfmark.PerfMark.startEnabled=true",
    )
}

tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/java9/**")
}
