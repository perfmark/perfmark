rootProject.name = "perfmark"
include(":perfmark-agent")
include(":perfmark-api")
include(":perfmark-api-testing")
include(":perfmark-examples")
include(":perfmark-impl")
include(":perfmark-java6")
include(":perfmark-java7")
include(":perfmark-java9")
include(":perfmark-java15")
include(":perfmark-java19")
include(":perfmark-testing")
include(":perfmark-tracewriter")
include(":perfmark-traceviewer")

project(":perfmark-agent").projectDir = File("$rootDir/agent")
project(":perfmark-api").projectDir =  File("$rootDir/api")
project(":perfmark-api-testing").projectDir =  File("$rootDir/api/testing")
project(":perfmark-examples").projectDir =  File("$rootDir/examples")
project(":perfmark-impl").projectDir =  File("$rootDir/impl")
project(":perfmark-java6").projectDir =  File("$rootDir/java6")
project(":perfmark-java7").projectDir =  File("$rootDir/java7")
project(":perfmark-java9").projectDir =  File("$rootDir/java9")
project(":perfmark-java15").projectDir =  File("$rootDir/java15")
project(":perfmark-java19").projectDir =  File("$rootDir/java19")
project(":perfmark-testing").projectDir =  File("$rootDir/testing")
project(":perfmark-tracewriter").projectDir =  File("$rootDir/tracewriter")
project(":perfmark-traceviewer").projectDir =  File("$rootDir/traceviewer")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("jmh", "1.36")

            library("junit", "junit:junit:4.13.2")
            library("errorprone", "com.google.errorprone:error_prone_annotations:2.32.0")
            library("truth", "com.google.truth:truth:1.4.4")

            library("jmhcore", "org.openjdk.jmh", "jmh-core").versionRef("jmh")
            library("jmhanno", "org.openjdk.jmh", "jmh-generator-annprocess").versionRef("jmh")

            version("gjf", "1.18.1")

            plugin("spotless", "com.diffplug.spotless").version("6.15.0")
            plugin("errorprone", "net.ltgt.errorprone").version("3.0.1")
            plugin("jmh", "me.champeau.jmh").version("0.6.8")
            plugin("jcstress", "io.github.reyerizo.gradle.jcstress").version("0.8.15")

        }
    }
}
