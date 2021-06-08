buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.testing")
    }
}

val jdkVersion = JavaVersion.VERSION_11

description = "PerfMark Testing"

dependencies {
    val libraries = project.ext.get("libraries") as Map<String, String>

    implementation(project(":perfmark-api"))
    implementation(project(":perfmark-impl"))
    implementation(libraries["junit"]!!)
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
}

tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/testing/**")
}
