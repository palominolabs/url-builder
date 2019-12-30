import java.util.Date

plugins {
    `java-library`
    id("groovy")
    id("maven-publish")

    id("com.github.ben-manes.versions") version "0.27.0"
    id("com.jfrog.bintray") version "1.8.4"
    id("com.github.spotbugs") version "3.0.0"
    id("me.champeau.gradle.jmh") version "0.5.0"
    id("net.researchgate.release") version "2.8.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    jcenter()
}

val depVersions by extra {
    mapOf(
            "slf4j" to "1.7.30",
            "jmh" to "1.22"
    )
}

dependencies {
    api("com.google.code.findbugs:jsr305:3.0.2")

    testRuntimeOnly("org.slf4j:slf4j-simple:${depVersions["slf4j"]}")
    testRuntimeOnly("org.slf4j:log4j-over-slf4j:${depVersions["slf4j"]}")
    testRuntimeOnly("org.slf4j:jcl-over-slf4j:${depVersions["slf4j"]}")
    testImplementation("org.slf4j:jul-to-slf4j:${depVersions["slf4j"]}")

    testImplementation("junit:junit:4.12")

    testImplementation("org.codehaus.groovy:groovy-all:2.4.4")

    jmhImplementation("com.google.guava:guava:27.1-jre")
}

tasks {
    register<Jar>("sourceJar") {
        from(project.the<SourceSetContainer>()["main"].allJava)
        archiveClassifier.set("sources")
    }

    register<Jar>("docJar") {
        from(project.tasks["javadoc"])
        archiveClassifier.set("javadoc")
    }
}

group = "com.palominolabs.http"

publishing {
    publications {
        register<MavenPublication>("bintray") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
            artifact(tasks["sourceJar"])
            artifact(tasks["docJar"])
        }
    }
}

bintray {
    user = rootProject.findProperty("bintrayUser")?.toString()
    key = rootProject.findProperty("bintrayApiKey")?.toString()
    setPublications("bintray")

    with(pkg) {
        repo = "maven"
        setLicenses("Copyfree")
        vcsUrl = "https://github.com/palominolabs/url-builder"
        name = "com.palominolabs.http:url-builder"

        with(version) {
            name = project.version.toString()
            released = Date().toString()
            vcsTag = "v" + project.version
        }
    }
}

spotbugs {
    // don't findbugs the tests
    sourceSets = listOf(project.sourceSets.named("main").get())
}

jmh {
    jmhVersion = depVersions["jmh"]
}

release {
    tagTemplate = "v\$version"
}

tasks.afterReleaseBuild {
    dependsOn(tasks.bintrayUpload)
}
