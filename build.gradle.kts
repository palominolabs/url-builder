import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.time.Duration

plugins {
    `java-library`
    kotlin("jvm") version "1.4.32"
    id("maven-publish")
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("com.github.ben-manes.versions") version "0.38.0"
    id("me.champeau.gradle.jmh") version "0.5.3"
    id("net.researchgate.release") version "2.8.1"
    id("org.jmailen.kotlinter") version "3.4.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

val deps by extra {
    mapOf(
        "slf4j" to "1.7.30",
        "jmh" to "1.22",
        "junit" to "5.7.1"
    )
}

dependencies {
    api("com.google.code.findbugs", "jsr305", "3.0.2")

    testRuntimeOnly("org.slf4j", "slf4j-simple", "${deps["slf4j"]}")
    testRuntimeOnly("org.slf4j", "log4j-over-slf4j", "${deps["slf4j"]}")
    testRuntimeOnly("org.slf4j", "jcl-over-slf4j", "${deps["slf4j"]}")
    testImplementation("org.slf4j", "jul-to-slf4j", "${deps["slf4j"]}")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "${deps["junit"]}")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "${deps["junit"]}")

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test-junit5"))


    jmhImplementation("com.google.guava", "guava", "27.1-jre")
}

group = "com.palominolabs.http"

tasks {
    test {
        useJUnitPlatform()
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

publishing {
    publications {
        register<MavenPublication>("sonatype") {
            from(components["java"])

            // sonatype required pom elements
            pom {
                name.set("${project.group}:${project.name}")
                description.set(name)
                url.set("https://github.com/palominolabs/url-builder")
                licenses {
                    license {
                        name.set("Copyfree Open Innovation License 0.4")
                        url.set("https://copyfree.org/content/standard/licenses/coil/license.txt")
                    }
                }
                developers {
                    developer {
                        id.set("marshallpierce")
                        name.set("Marshall Pierce")
                        email.set("575695+marshallpierce@users.noreply.github.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/palominolabs/url-builder")
                    developerConnection.set("scm:git:ssh://git@github.com:palominolabs/url-builder.git")
                    url.set("https://github.com/palominolabs/url-builder")
                }
            }
        }
    }

    // A safe throw-away place to publish to:
    // ./gradlew publishSonatypePublicationToLocalDebugRepository -Pversion=foo
    repositories {
        maven {
            name = "localDebug"
            url = URI.create("file:///${project.buildDir}/repos/localDebug")
        }
    }
}

jmh {
    jmhVersion = deps["jmh"]
}

tasks.afterReleaseBuild {
    dependsOn(provider { project.tasks.named("publishToSonatype") })
}

// don't barf for devs without signing set up
if (project.hasProperty("signing.keyId")) {
    signing {
        sign(project.extensions.getByType<PublishingExtension>().publications["sonatype"])
    }
}

nexusPublishing {
    repositories {
        sonatype {
            // sonatypeUsername and sonatypePassword properties are used automatically
            stagingProfileId.set("26c8b7fff47581") // com.palominolabs
        }
    }
    // these are not strictly required. The default timeouts are set to 1 minute. But Sonatype can be really slow.
    // If you get the error "java.net.SocketTimeoutException: timeout", these lines will help.
    connectTimeout.set(Duration.ofMinutes(3))
    clientTimeout.set(Duration.ofMinutes(3))
}
