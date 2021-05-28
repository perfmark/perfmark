buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.apitesting")
    }
}

description = "PerfMark API Tests"

dependencies {
    val libraries = project.ext.get("libraries") as Map<String, String>
    testImplementation(libraries["truth"]!!)

    testImplementation(project(":perfmark-api"))
    testImplementation(project(":perfmark-tracewriter"))
    testImplementation(project(":perfmark-traceviewer"))
    testImplementation(project(":perfmark-agent"))
    testRuntimeOnly(project(":perfmark-java6"))
    testRuntimeOnly(project(":perfmark-java7"))
    testRuntimeOnly(project(":perfmark-java9"))
}

tasks.named<JavaCompile>("compileTestJava") {
    sourceCompatibility = JavaVersion.VERSION_16.toString()
    targetCompatibility = JavaVersion.VERSION_16.toString()
}
