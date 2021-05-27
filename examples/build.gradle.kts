plugins {
    application
}

buildscript {
    extra.apply {
        set("moduleName", "io.perfmark.examples")
    }
}

val jdkVersion = JavaVersion.VERSION_1_8

dependencies {
    implementation(project(":perfmark-api"))
    implementation(project(":perfmark-tracewriter"))
    runtimeOnly(project(":perfmark-java7"))
    runtimeOnly(project(":perfmark-java6"))
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
}


application {
    mainClass.set("io.perfmark.examples.perfetto.WebServer")
    applicationDefaultJvmArgs = mutableListOf(
            // "-javaagent:" + configurations.perfmarkAgent.singleFile.path,
            "-Xlog:class+load=info",
            "-Dio.perfmark.PerfMark.startEnabled=true",
    )
}

tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/java9/**")
}
