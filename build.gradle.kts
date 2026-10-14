import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

plugins {
    id("net.ltgt.errorprone") version "4.4.0" apply false
}

@Suppress("UNCHECKED_CAST")
val errorProneEnabled: Boolean
    get() = when (val prop = rootProject.findProperty("errorProne")) {
        null -> true
        is Boolean -> prop
        is String -> prop.isNotEmpty()
        else -> true
    }

// Capture Junit here since subproject can't access it yet.
val junitLib = libs.junit

subprojects {
    apply(plugin = "checkstyle")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "idea")
    apply(plugin = "signing")
    apply(plugin = "net.ltgt.errorprone")

    repositories {
        maven {
            url = uri("https://maven-central.storage-download.googleapis.com/repos/central/data/")
        }
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    val javadocJar = tasks.register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        from(tasks.named("javadoc"))
    }

    // Resolved here (not inside the task block) because `the<...>()` inside a task configuration
    // lambda would look at the task's own extension container.
    val mainAllSource = the<JavaPluginExtension>().sourceSets["main"].allSource

    val sourcesJar = tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(mainAllSource)
    }

    configure<CheckstyleExtension> {
        configDirectory.set(rootProject.file("$rootDir/buildscripts"))
        toolVersion = "6.17"
        // `ignoreFailures` cannot be used as a Kotlin property: the private field of the same
        // name shadows the accessor pair.
        setIgnoreFailures(false)
        if (rootProject.hasProperty("checkstyle.ignoreFailures")) {
            setIgnoreFailures(rootProject.findProperty("checkstyle.ignoreFailures").toString().toBoolean())
        }
    }

    afterEvaluate {
        tasks.named<Jar>("jar") {
            manifest {
                attributes(
                    mapOf(
                        // `project.extra`, not `extra`: inside a task block the receiver is the
                        // task, which has its own (empty) extension container.
                        "Automatic-Module-Name" to project.extra["moduleName"],
                        "Implementation-Version" to archiveVersion.get(),
                        "Implementation-Title" to "PerfMark (https://www.perfmark.io/)",
                        "Implementation-Vendor" to
                            "Carl Mastrangelo https://www.carlmastrangelo.com/ https://twitter.com/CarlMastrangelo",
                        "Specification-Version" to archiveVersion.get(),
                        "Specification-Title" to "PerfMark (https://www.perfmark.io/)",
                        "Specification-Vendor" to "Carl Mastrangelo (https://www.perfmark.io/)",
                        "Carl-Is-Awesome" to "true"
                    )
                )
            }
        }
    }

    configure<PublishingExtension> {
        publications {
            register<MavenPublication>("maven") {
                from(components["java"])

                // `.get()` on purpose: the Groovy script handed the *Task* to `artifact`, which
                // makes Gradle create an ArchiveTaskBasedMavenArtifact (a TaskProvider would
                // produce a different artifact type).
                artifact(javadocJar.get())
                artifact(sourcesJar.get())

                pom {
                    val pomSelf = this
                    name.set("${project.group}:${project.name}")
                    url.set("https://github.com/perfmark/perfmark")
                    afterEvaluate {
                        // description is not available until evaluated.
                        pomSelf.description.set(project.description)
                    }

                    scm {
                        connection.set("scm:git:https://github.com/perfmark/perfmark.git")
                        developerConnection.set("scm:git@github.com:perfmark/perfmark.git")
                        url.set("https://github.com/perfmark/perfmark")
                    }

                    licenses {
                        license {
                            name.set("Apache 2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("carl-mastrangelo")
                            name.set("Carl Mastrangelo")
                            email.set("carl@carlmastrangelo.com")
                            url.set("https://www.perfmark.io/")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                val stagingUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                val releaseUrl = stagingUrl
                val snapshotUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
                // Read at configuration time, exactly like the Groovy original: `version` is
                // still "unspecified" here because it is assigned further down.
                url = uri(if (project.version.toString().endsWith("SNAPSHOT")) snapshotUrl else releaseUrl)
                credentials {
                    if (rootProject.hasProperty("ossrhUsername")
                        && rootProject.hasProperty("ossrhPassword")
                    ) {
                        username = rootProject.findProperty("ossrhUsername") as String?
                        password = rootProject.findProperty("ossrhPassword") as String?
                    }
                }
            }
        }
    }

    configure<SigningExtension> {
        isRequired = false
        sign(the<PublishingExtension>().publications["maven"])
    }

    // `name` in the Groovy closure resolved against the Project (Groovy OWNER_FIRST), not the
    // task, so the project name is used explicitly here.
    tasks.matching {
        it.name == "publishMavenPublicationToMavenRepository" || it.name == "publishMavenPublicationToMavenLocal"
    }.configureEach {
        onlyIf {
            !project.name.contains("perfmark-examples") && !project.name.contains("perfmark-api-testing")
                && !project.name.contains("perfmark-testing") && !project.name.contains("perfmark-agent")
                && !project.name.contains("perfmark-java19")
        }
    }

    tasks.named("javadoc") {
        onlyIf {
            !project.name.contains("perfmark-java9") && !project.name.contains("perfmark-examples")
                && !project.name.contains("perfmark-api-testing") && !project.name.contains("perfmark-testing")
        }
    }

    if (errorProneEnabled) {
        dependencies {
            add("errorprone", "com.google.errorprone:error_prone_core:2.29.2")
            add("errorproneJavac", "com.google.errorprone:javac:9+181-r4173-1")
        }
    } else {
        // Disable Error Prone
        allprojects {
            afterEvaluate {
                tasks.withType<JavaCompile>().configureEach {
                    options.errorprone.isEnabled.set(false)
                }
            }
        }
    }

    group = "io.perfmark"
    version = "0.28.0-SNAPSHOT"

    dependencies {
        // No type-safe `testImplementation` accessor exists inside `subprojects {}` because
        // java-library is applied dynamically.
        add("testImplementation", junitLib)
    }
}
