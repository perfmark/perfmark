buildscript {
    extra.apply{
        set("moduleName", "io.perfmark.testing")
    }
}

val jdkVersion = JavaVersion.VERSION_11

description = "PerfMark Testing"

dependencies {
    implementation(project(":perfmark-api"))
    implementation(project(":perfmark-impl"))
    implementation(libs.jmhcore)
    implementation(libs.junit)
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = jdkVersion.toString()
    targetCompatibility = jdkVersion.toString()
}

tasks.named<Javadoc>("javadoc") {
    exclude("io/perfmark/testing/**")
}
